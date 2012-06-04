/*jslint
    onevar: false, undef: true, newcap: true, nomen: false, es5: true,
    regexp: false, plusplus: true, bitwise: false, browser: true, indent: 4 */
/*global _, blitz, $, Math, prettyPrint, jQuery */
this.blitz = this.blitz || {};

blitz.pp = {
    number: (function () {
        var RE_COMMIFY = /(\d)(?=(\d\d\d)+(?!\d))/g;
        return function (number) {
            return number.toString().replace(RE_COMMIFY, "$1,");
        };
    }()),
    bytes: (function () {
        var gb = 1024 * 1024 * 1024;
        var mb = 1024 * 1024;
        var kb = 1024;
        return function (bytes) {
            if (bytes >= gb) {
                return (bytes / gb).toFixed(2) + ' GB';
            } else if (bytes >= mb) {
                return (bytes / mb).toFixed(2) + ' MB';
            } else if (bytes >= kb) {
                return (bytes / kb).toFixed(2) + ' KB';
            } else {
                return bytes + ' bytes';
            }
        };
    }()),
    percent: function (percent) {
        if (percent === 0.0) {
            return '0';
        } else {
            return percent.toFixed(2);
        }
    },
    duration: function (time) {
        if (time < 1.0) {
            return Math.floor(time * 1000) + ' ms';
        } else if (time >= 60.0) {
            return (time / 60.0).toFixed(1) + ' min';
        } else if (time === 1.0) {
            return time + ' second';
        } else {
            return time.toFixed(2) + ' seconds';
        }
    },
    rate: function (rate) {
        if (rate < 10.0) {
            return rate.toFixed(2) + '/second';
        } else {
            return Math.floor(rate) + '/second';
        }
    },
    html: function (text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }
};

blitz.curl = (function () {
    function _plot($root, test, pattern, contextPath) {
        var html = [];
        html.push('<div>');

//        html.push('<ul class="tab">');
//        html.push('<li ><a class="tab" id="highlights" href="#"><span>Highlights</span></a></li>');
//        html.push('<li class="active"><a class="tab" id="rush" href="#"><span>Rush</span></a></li>');
//        html.push('</ul>');

        html.push('<div class="tab content" id="highlights">');
        html.push('<p>Highlights are not available while the game is on. Please wait...</p>');
        html.push('</div>');

        html.push('<div class="tab content" id="rush">');
        html.push('<div class="title"><div class="rt legend" /><div>Response Times</div></div>');
        html.push('<div class="chart-outer"><div id="rt" class="chart" style="height:300px;width:875px;"/></div>');
        html.push('<div style="height:20px"/>');
        html.push('<div class="title"><div class="rate legend"/><div>Hit Rate</div></div>');
        html.push('<div class="chart-outer"><div id="rate" class="chart" style="height:300px;width:875px;"/></div>');
        html.push('</div>');

        html.push('<p/>');
        html.push('</div>');
        $root.html(html.join('')).fadeIn()
            .find('a.tab').click(function () {
                var id = jQuery(this).attr('id');
                $root.find('ul.tab li.active').removeClass('active').end();
                jQuery(this).parent('li').addClass('active');
                $root.find('div.tab').fadeOut().end().find('div.tab#' + id).fadeIn();
                return false;
            }).end();

        test.$rt_chart = $root.find('div.chart#rt');
        test.$rt_legend = $root.find('div.rt.legend');
        test.$rate_chart = $root.find('div.chart#rate');
        test.$rate_legend = $root.find('div.rate.legend');
        test.$highlights = $root.find('div#highlights');

        test.series = {
            volume: {
                label: 'users',
                data: [[ 0.0, pattern.intervals[0].start ]],
                color: '#707070',
                yaxis: 2,
                lines: {show: true, fill: 0.1}
            },
            requests: {
                label: 'hits/sec',
                data: [],
                color: '#006400',
                lines: {show: true},
                points: {show: true, radius: 2},
                shadowSize: 4
            },
            timeouts: {
                label: 'timeouts/sec',
                data: [],
                color: '#E66C25',
                lines: {show: true},
                points: {show: true, radius: 2},
                shadowSize: 4
            },
            errors: {
                label: 'errors/sec',
                data: [],
                color: '#DD1122',
                lines: {show: true},
                points: {show: true, radius: 2},
                shadowSize: 4
            },
            duration: {
                label: 'response',
                data: [],
                color: '#B1D81C',
                lines: {show: true},
                points: {show: true, radius: 4},
                shadowSize: 8
            }
        };

        var i, interval;
        test.max_duration = 0.0;
        test.max_volume = 0.0;
        for (i = 0; i < pattern.intervals.length; i += 1) {
            interval = pattern.intervals[i];
            test.max_duration += interval.duration * (interval.iterations || 1);
            test.max_volume = Math.max(test.max_volume, Math.max(interval.start, interval.end));
        }
        var rt_options = {
            grid: {
                autoHighlight: true,
                hoverable: true,
                labelMargin: 10,
                borderWidth: 0,
                backgroundColor: {
                    colors: ["#f5f5f5", "#ffffff"]
                }
            },
            xaxis: {
                min: 0,
                max: test.max_duration,
                tickFormatter: function (val, axis) {
                    if (val === 0.0) {
                        return '';
                    } else if (val >= 60) {
                        return (val / 60).toFixed(1) + ' min';
                    } else {
                        return val.toFixed(0) + ' sec';
                    }
                }
            },
            yaxis: {
                min: 0,
                labelHeight: 8,
                tickFormatter: function (val, axis) {
                    if (val === 0) {
                        return '';
                    } else if (val < 1.0) {
                        return Math.floor(val * 1000) + ' ms';
                    } else {
                        return val.toFixed(2) + ' sec';
                    }
                }
            },
            y2axis: {
                min: 0,
                max: test.max_volume,
                labelHeight: 8,
                tickFormatter: function (val, axis) {
                    val = Math.floor(val);
                    return val === 0 ? '' : (blitz.pp.number(val) + ' users');
                }
            },
            crosshair: {
                mode: "xy",
                color: "rgba(100,183,230,0.2)"
            },
            legend: {show: false}
        };

        var rate_options = {
            grid: {
                autoHighlight: true,
                hoverable: true,
                labelMargin: 10,
                borderWidth: 0,
                backgroundColor: {
                    colors: ["#f5f5f5", "#ffffff"]
                }
            },
            xaxis: {
                min: 0,
                max: test.max_duration,
                tickFormatter: function (val, axis) {
                    return "";
                }
            },
            yaxis: {
                min: 0,
                labelHeight: 8,
                tickFormatter: function (val, axis) {
                    return val === 0 ? '' : val.toFixed(0) + '/sec';
                }
            },
            y2axis: {
                min: 0,
                max: test.max_volume,
                labelHeight: 8,
                tickFormatter: function (val, axis) {
                    val = Math.floor(val);
                    return val === 0 ? '' : (blitz.pp.number(val) + ' users');
                }
            },
            crosshair: {
                mode: "xy",
                color: "rgba(100,183,230,0.2)"
            },
            legend: {show: false}
        };

        var _legend = function (index) {
            if (index) {
                var _d = test.series.duration.data[index][1], _d2;
                var _v = test.series.volume.data[index][1], _v2;
                if (_d === 0) {
                    _d2 = '';
                } else if (_d < 1.0) {
                    _d2 = '<span class="blue">' + Math.floor(_d * 1000) + '</span> ms @ ';
                } else {
                    _d2 = '<span class="blue">' + _d.toFixed(2) + '</span> sec @ ';
                }

                if (_v === 0) {
                    _v2 = '';
                } else {
                    _v2 = '<span class="blue">' + blitz.pp.number(_v) + '</span> users';
                }
                test.$rt_legend.html(_d2 + _v2);

                var _ary = [];
                var _hs = Math.floor(test.series.requests.data[index][1]);
                var _es = Math.floor(test.series.errors.data[index][1]);
                var _ts = Math.floor(test.series.timeouts.data[index][1]);
                _ary.push('<span class="hits">' + _hs + '</span> hits/sec');
                if (_es) {
                    _ary.push('<span class="errors">' + _es + '</span> errors/sec');
                }
                if (_ts) {
                    _ary.push('<span class="timeouts">' + _ts + '</span> timeouts/sec');
                }
                test.$rate_legend.html(_ary.join(' | '));
            } else {
                test.$rt_legend.empty();
                test.$rate_legend.empty();
            }
        };
        if (test.timeline) {
            test.series.volume.data.length   = 1;
            test.series.duration.data.length = 0;
            test.series.requests.data.length = 0;
            test.series.timeouts.data.length = 0;
            //console.info(test);
            jQuery.ajax({
                url: contextPath+'/plugin/blitz_io-jenkins/templates/rush.html',
                dataType: 'text',
                success: function (data) {
                    test.template = _.template(data);
                    test.$highlights.html(test.template({test: test}));
                }
            });

            var last;
            jQuery.each(test.timeline, function (i, r) {
                test.series.volume.data.push([ r.timestamp, r.volume ]);
                if (r.duration < 0.0) {
                    r.duration = 0.0;
                }
                if (i === 0) {
                    test.series.duration.data.push([ 0.0, r.duration ]);
                }
                test.series.duration.data.push([ r.timestamp, r.duration ]);
                if (last) {
                    var elapsed = r.timestamp - last.timestamp;
                    var lreqs = last.hits;
                    var sreqs = r.hits;
                    var rreqs = (sreqs - lreqs) / elapsed;
                    if (test.series.requests.data.length === 0) {
                        test.series.requests.data.push([ 0.0, rreqs ]);
                    }
                    test.series.requests.data.push([ r.timestamp, rreqs ]);

                    var ltouts = last.timeouts;
                    var stouts = r.timeouts;
                    var rtouts = (stouts - ltouts) / elapsed;
                    if (test.series.timeouts.data.length === 0) {
                        test.series.timeouts.data.push([ 0.0, rtouts ]);
                    }
                    test.series.timeouts.data.push([ r.timestamp, rtouts ]);
                    if (stouts && !test.first_timeout_point) {
                        test.first_timeout_point = r;
                    }

                    var lerrs = last.errors;
                    var serrs = r.errors;
                    var rerrs = (serrs - lerrs) / elapsed;
                    if (test.series.errors.data.length === 0) {
                        test.series.errors.data.push([ 0.0, rerrs ]);
                    }
                    test.series.errors.data.push([ r.timestamp, rerrs ]);
                    if (serrs && !test.first_error_point) {
                        test.first_error_point = r;
                    }
                }
                last = r;
            });

            test.last_point = last;
            test.average_hit_rate = _.reduce(test.series.requests.data, function (memo, pt) {
                return memo + pt[1];
            }, 0) / (test.series.requests.data.length || 1);
            test.average_response_time = _.reduce(test.timeline, function (memo, pt) {
                return memo + pt.duration;
            }, 0) / (test.timeline.length || 1);
        }
        test.$rt_plot = jQuery.plot(test.$rt_chart, [
            test.series.duration,
            test.series.volume
        ], rt_options);

        test.$rt_chart.bind('plothover', function (event, pos, item) {
            _legend(item ? item.dataIndex : undefined);
        });

        test.$rate_plot = jQuery.plot(test.$rate_chart, [
            test.series.requests,
            test.series.timeouts,
            test.series.errors,
            test.series.volume
        ], rate_options);

        test.$rate_chart.bind('plothover', function (event, pos, item) {
            _legend(item ? item.dataIndex : undefined);
        });
    }

    return {
        plot: _plot
    };
}());

blitz.curl.test = (function () {
    return function (t, p, contextPath) {
        var test = jQuery.parseJSON(t) || { steps: [] },
            pattern = jQuery.parseJSON(p) || { iterations: 1, intervals: [] };
        contextPath = contextPath || '';
        return {
            render: function($root) {
                blitz.curl.plot($root, test, pattern, contextPath);
            }
        };
    };
}());
