package org.zalando.zmon.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.zalando.sprocwrapper.proxy.SProcCallHandler;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zalando.zmon.api.domain.EntityObject;
import org.zalando.zmon.api.domain.ResourceNotFoundException;
import org.zalando.zmon.persistence.EntitySProcService;
import org.zalando.zmon.security.permission.DefaultZMonPermissionService;

import java.lang.Double;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 1/28/15.
 */
@Controller
@RequestMapping("/api/v1/entities")
public class EntityApi {

    private final Logger log = LoggerFactory.getLogger(EntityApi.class);

    private final MetricRegistry metricRegistry;

    EntitySProcService entitySprocs;

    ObjectMapper mapper;

    DefaultZMonPermissionService authService;

    @Autowired
    public EntityApi(EntitySProcService entitySprocs, ObjectMapper mapper, MetricRegistry metricRegistry, DefaultZMonPermissionService authService) {
        this.entitySprocs = entitySprocs;
        this.mapper = mapper;
        this.authService = authService;
        this.metricRegistry = metricRegistry;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleCheckConstraintViolation(DataIntegrityViolationException exception) {
        if (exception.getCause() instanceof PSQLException && exception.getMessage().contains("violates check constraint")) {
            return new ResponseEntity<>("Check constraint violated", HttpStatus.BAD_REQUEST);
        }
        throw exception;
    }

    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @RequestMapping(value = {"/", ""}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<String> addEntity(@RequestBody JsonNode entity) {

        if (!entity.has("type")) {
            return new ResponseEntity<>("Creating entity without a type is not possible.", HttpStatus.BAD_REQUEST);
        }
        if (!entity.has("id")) {
            return new ResponseEntity<>("Creating entity without an id is not possible.", HttpStatus.BAD_REQUEST);
        }

        if ("global".equals(entity.get("type").textValue().toLowerCase())) {
            return new ResponseEntity<>("Creating entity with type 'GLOBAL' is not allowed.", HttpStatus.FORBIDDEN);
        }
        if ("global".equals(entity.get("id").textValue().toLowerCase())) {
            return new ResponseEntity<>("Creating entity with id 'GLOBAL' is not allowed.", HttpStatus.FORBIDDEN);
        }

        if ("zmon_config".equals(entity.get("type").textValue())) {
            if (!authService.hasAdminAuthority()) {
                throw new AccessDeniedException("No ADMIN privileges present to update configuration.");
            }
            log.info("Modifying config entity: id={} user={}", entity.get("id"), authService.getUserName());
        }

        try {
            String data = mapper.writeValueAsString(entity);
            String id = entitySprocs.createOrUpdateEntity(data, Lists.newArrayList(authService.getTeams()), authService.getUserName());
            if (id == null) {
                throw new AccessDeniedException("Access denied: entity was not updated");
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException ex) {
            log.error("Entity not serializable", ex);
            return new ResponseEntity<>("Entity not serializable.", HttpStatus.BAD_REQUEST);
        } catch (org.springframework.jdbc.UncategorizedSQLException ex) {
            log.error("attempt to modify type: ", ex);
            org.postgresql.util.PSQLException rc = (PSQLException) ex.getRootCause();

            return new ResponseEntity<>(rc.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @RequestMapping(value = {"/", ""}, method = RequestMethod.GET)
    public List<EntityObject> getEntities(@RequestParam(value = "query", defaultValue = "[{}]") String data, @RequestParam(value = "exclude", defaultValue = "") String exclude) throws IOException {
        List<String> entitiesString;

        if (!exclude.isEmpty()) {
            entitiesString = entitySprocs.getEntitiesWithoutTag(exclude);
        } else {
            if (data.startsWith("{")) {
                data = "[" + data + "]";
            }

            entitiesString = entitySprocs.getEntities(data);
        }
        List<EntityObject> list = new ArrayList<>(entitiesString.size());

        for (String e : entitiesString) {
            EntityObject o = mapper.readValue(e, EntityObject.class);
            list.add(o);
        }

        return list;
    }

    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @RequestMapping(value = {"/{id}/", "/{id}"})
    public void getEntity(@PathVariable(value = "id") String id, final Writer writer) {
        List<String> entities = entitySprocs.getEntityById(id);
        if (entities.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        try {
            for (String s : entities) {
                writer.write(s);// there is at most one entity
            }
        } catch (IOException ex) {
            log.error("", ex);
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @RequestMapping(value = {"/{id}/", "/{id}"}, method = RequestMethod.DELETE)
    public int deleteEntity(@PathVariable(value = "id") String id) {
        List<String> teams = Lists.newArrayList(authService.getTeams());
        List<String> deleted = entitySprocs.deleteEntity(id, teams, authService.getUserName());

        if (!deleted.isEmpty()) {
            try {
                JsonNode e = mapper.readValue(deleted.get(0), JsonNode.class);
                String type = e.get("type").textValue().toLowerCase();
                Double created = new Double(e.get("created").doubleValue() * 1000);
                long duration = (new Date()).getTime() - created.longValue();
                Timer timer = metricRegistry.timer("controller.entity-lifetime." + type);
                timer.update(duration, TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                log.error("", ex);
            }
            log.info("Deleted entity {} by user {} with teams {} => {})", id, authService.getUserName(), teams, deleted.get(0));
        }
        return deleted.size();
    }

}
