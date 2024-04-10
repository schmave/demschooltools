jest.dontMock('../src/js/reportstore');
jest.dontMock('../src/js/appconstants');
jest.dontMock('../src/js/storebase');
jest.dontMock('object-assign');


describe('ReportStore', function () {

    constants = require('../src/js/appconstants');

    var yearLoadedMock = {
        type: constants.reportEvents.YEARS_LOADED,
        data: ['some', 'data']
    };

    var dispatcher,
        reportStore,
        callback;

    beforeEach(function () {
        dispatcher = require('../src/js/appdispatcher');
        reportStore = require('../src/js/reportstore');
        callback = dispatcher.register.mock.calls[0][0];
    });

    it('registers a callback with the dispatcher', function () {
        expect(dispatcher.register.mock.calls.length).toBe(1);
    });

    it('returns undefined if years have not been loaded', function(){
        var years = reportStore.getSchoolYears();
        expect(years).toBe(undefined);
    });

    it('calls the action creator if years have not been loaded yet', function(){
        callback(yearLoadedMock);
        var years = reportStore.getSchoolYears();

        expect(years).toEqual(['some', 'data']);
    })

    //case constants.reportEvents.YEARS_LOADED:
    //schoolYears = action.data;
    //exports.emitChange();
    //break;
    //case constants.reportEvents.REPORT_LOADED:
    //reports[action.data.year] = action.data.report;
    //exports.emitChange();
    //break;
    //case constants.reportEvents.PERIOD_CREATED:
    //exports.getSchoolYears(true);
    //break;
//}
});