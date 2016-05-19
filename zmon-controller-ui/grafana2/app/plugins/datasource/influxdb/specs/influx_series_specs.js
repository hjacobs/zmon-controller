///<amd-dependency path="app/plugins/datasource/influxdb/influx_series" name="InfluxSeries"/>
define(["require", "exports", "app/plugins/datasource/influxdb/influx_series", 'test/lib/common'], function (require, exports, InfluxSeries, common_1) {
    common_1.describe('when generating timeseries from influxdb response', function () {
        common_1.describe('given multiple fields for series', function () {
            var options = {
                alias: '',
                series: [
                    {
                        name: 'cpu',
                        tags: { app: 'test', server: 'server1' },
                        columns: ['time', 'mean', 'max', 'min'],
                        values: [[1431946625000, 10, 11, 9], [1431946626000, 20, 21, 19]]
                    }
                ]
            };
            common_1.describe('and no alias', function () {
                common_1.it('should generate multiple datapoints for each column', function () {
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result.length).to.be(3);
                    common_1.expect(result[0].target).to.be('cpu.mean {app: test, server: server1}');
                    common_1.expect(result[0].datapoints[0][0]).to.be(10);
                    common_1.expect(result[0].datapoints[0][1]).to.be(1431946625000);
                    common_1.expect(result[0].datapoints[1][0]).to.be(20);
                    common_1.expect(result[0].datapoints[1][1]).to.be(1431946626000);
                    common_1.expect(result[1].target).to.be('cpu.max {app: test, server: server1}');
                    common_1.expect(result[1].datapoints[0][0]).to.be(11);
                    common_1.expect(result[1].datapoints[0][1]).to.be(1431946625000);
                    common_1.expect(result[1].datapoints[1][0]).to.be(21);
                    common_1.expect(result[1].datapoints[1][1]).to.be(1431946626000);
                    common_1.expect(result[2].target).to.be('cpu.min {app: test, server: server1}');
                    common_1.expect(result[2].datapoints[0][0]).to.be(9);
                    common_1.expect(result[2].datapoints[0][1]).to.be(1431946625000);
                    common_1.expect(result[2].datapoints[1][0]).to.be(19);
                    common_1.expect(result[2].datapoints[1][1]).to.be(1431946626000);
                });
            });
            common_1.describe('and simple alias', function () {
                common_1.it('should use alias', function () {
                    options.alias = 'new series';
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result[0].target).to.be('new series');
                    common_1.expect(result[1].target).to.be('new series');
                    common_1.expect(result[2].target).to.be('new series');
                });
            });
            common_1.describe('and alias patterns', function () {
                common_1.it('should replace patterns', function () {
                    options.alias = 'alias: $m -> $tag_server ([[measurement]])';
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result[0].target).to.be('alias: cpu -> server1 (cpu)');
                    common_1.expect(result[1].target).to.be('alias: cpu -> server1 (cpu)');
                    common_1.expect(result[2].target).to.be('alias: cpu -> server1 (cpu)');
                });
            });
        });
        common_1.describe('given measurement with default fieldname', function () {
            var options = { series: [
                    {
                        name: 'cpu',
                        tags: { app: 'test', server: 'server1' },
                        columns: ['time', 'value'],
                        values: [["2015-05-18T10:57:05Z", 10], ["2015-05-18T10:57:06Z", 12]]
                    },
                    {
                        name: 'cpu',
                        tags: { app: 'test2', server: 'server2' },
                        columns: ['time', 'value'],
                        values: [["2015-05-18T10:57:05Z", 15], ["2015-05-18T10:57:06Z", 16]]
                    }
                ] };
            common_1.describe('and no alias', function () {
                common_1.it('should generate label with no field', function () {
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result[0].target).to.be('cpu {app: test, server: server1}');
                    common_1.expect(result[1].target).to.be('cpu {app: test2, server: server2}');
                });
            });
        });
        common_1.describe('given two series', function () {
            var options = {
                alias: '',
                series: [
                    {
                        name: 'cpu',
                        tags: { app: 'test', server: 'server1' },
                        columns: ['time', 'mean'],
                        values: [[1431946625000, 10], [1431946626000, 12]]
                    },
                    {
                        name: 'cpu',
                        tags: { app: 'test2', server: 'server2' },
                        columns: ['time', 'mean'],
                        values: [[1431946625000, 15], [1431946626000, 16]]
                    }
                ]
            };
            common_1.describe('and no alias', function () {
                common_1.it('should generate two time series', function () {
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result.length).to.be(2);
                    common_1.expect(result[0].target).to.be('cpu.mean {app: test, server: server1}');
                    common_1.expect(result[0].datapoints[0][0]).to.be(10);
                    common_1.expect(result[0].datapoints[0][1]).to.be(1431946625000);
                    common_1.expect(result[0].datapoints[1][0]).to.be(12);
                    common_1.expect(result[0].datapoints[1][1]).to.be(1431946626000);
                    common_1.expect(result[1].target).to.be('cpu.mean {app: test2, server: server2}');
                    common_1.expect(result[1].datapoints[0][0]).to.be(15);
                    common_1.expect(result[1].datapoints[0][1]).to.be(1431946625000);
                    common_1.expect(result[1].datapoints[1][0]).to.be(16);
                    common_1.expect(result[1].datapoints[1][1]).to.be(1431946626000);
                });
            });
            common_1.describe('and simple alias', function () {
                common_1.it('should use alias', function () {
                    options.alias = 'new series';
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result[0].target).to.be('new series');
                });
            });
            common_1.describe('and alias patterns', function () {
                common_1.it('should replace patterns', function () {
                    options.alias = 'alias: $m -> $tag_server ([[measurement]])';
                    var series = new InfluxSeries(options);
                    var result = series.getTimeSeries();
                    common_1.expect(result[0].target).to.be('alias: cpu -> server1 (cpu)');
                    common_1.expect(result[1].target).to.be('alias: cpu -> server2 (cpu)');
                });
            });
        });
        common_1.describe('given measurement with dots', function () {
            var options = {
                alias: '',
                series: [
                    {
                        name: 'app.prod.server1.count',
                        tags: {},
                        columns: ['time', 'mean'],
                        values: [[1431946625000, 10], [1431946626000, 12]]
                    }
                ]
            };
            common_1.it('should replace patterns', function () {
                options.alias = 'alias: $1 -> [[3]]';
                var series = new InfluxSeries(options);
                var result = series.getTimeSeries();
                common_1.expect(result[0].target).to.be('alias: prod -> count');
            });
        });
    });
});
//# sourceMappingURL=influx_series_specs.js.map