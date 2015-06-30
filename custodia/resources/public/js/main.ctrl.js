angular.module('app', ['ui.bootstrap', 'ngGrid']);
angular.module('app').directive("clickToEdit", function() {
    var editorTemplate = '<h1 class="click-to-edit col-md-6">' +
        '<span ng-hide="view.editorEnabled">' +
        '{{value}} ' +
        '<a id="edit-name" ng-click="enableEditor()">Edit</a>' +
        '</span>' +
        '<span ng-show="view.editorEnabled">' +
        '<input id="new-name" ng-model="view.editableValue">' +
        '<a id="save-name" href="#" ng-click="save()">Save</a>' +
        ' or ' +
        '<a id="cancel-name" ng-click="disableEditor()">Cancel</a>.' +
        '</span>' +
        '</h1>';

    return {
        restrict: "A",
        replace: true,
        template: editorTemplate,
        scope: {
            value: "=clickToEdit",
        },
        controller: function($scope) {
            $scope.saveName = $scope.$parent.saveName;
            $scope.view = {
                editableValue: $scope.value,
                editorEnabled: false
            };

            $scope.enableEditor = function() {
                $scope.view.editorEnabled = true;
                $scope.view.editableValue = $scope.value;
            };

            $scope.disableEditor = function() {
                $scope.view.editorEnabled = false;
            };

            $scope.save = function() {
                $scope.value = $scope.view.editableValue;
                $scope.disableEditor();
                $scope.saveName($scope.value);
            };
        }
    };
});
angular.module('app').filter('orderObjectBy', function() {
    return function(items, field, reverse) {
        var filtered = [];
        angular.forEach(items, function(item) {
            filtered.push(item);
        });
        filtered.sort(function (a, b) {
            return (a[field] > b[field] ? 1 : -1);
        });
        if(reverse) filtered.reverse();
        return filtered;
    };
});
angular.module('app').controller("MainController", function($scope, $http){
    $scope.students = {};
    $scope.screen = "home";
    $scope.current_totals_year = null;
    $scope.missing_date = "";
    $scope.missing_swipe = "";
    $scope.backStudent = function(s) {
        $scope.swipedWorked = false;
        $scope.screen = "student";
    };

    $scope.styleStudentCalendarDay = function(date, mode){
        if (mode ==="day") {
            var dayToCheck = new Date(date).setHours(0,0,0,0);
            if (dayToCheck === $scope.current_day) {
                return "red";
            }
        }
    };
    $scope.showStudent = function(s) {
        $scope.screen = "loading";
        $scope.swipedWorked = false;
        $scope.getStudent(s._id, function(s){
            $scope.backStudent();
            $scope.reloadStudentPage(s);
        });
    };
    $scope.getStudent = function(_id, fn) {
        $http.get('/student/' + _id).
            success(function(data){
                fn(data.student);
            }). error(function(){});
    };
    $scope.showHome = function(worked) {
        $scope.swipedWorked = worked;
        $scope.screen = "home";
    };
    $scope.showSwipedToday = function() {
        $scope.swipedWorked = false;
        $scope.screen = "swiped-today";
    };
    $scope.showCreate = function() {
        $scope.swipedWorked = false;
        $scope.screen = "create";};
    $scope.showCreateYear = function() {
        $scope.swipedWorked = false;
        $scope.screen = "create-year";};
    $scope.showStudents = function() {
        $scope.screen = "loading";
        $scope.getTotalsStudents();
    };
    $scope.setDay = function(s) {
        $scope.current_day = s;
    };
    $scope.gridOptions = { data: 'totals_students',
                           columnDefs: [{ field: 'name', displayName: 'Name'},
                                        { field: 'good', displayName: 'Attended (Overrides)'},
                                        { field: 'unexcused', displayName: 'Unexcused Absence'},
                                        { field: 'excuses', displayName: 'Excused Absence'},
                                        { field: 'short', displayName: 'Short'},
                                        { field: 'total_hours', displayName: 'Total Hours', cellTemplate: '<div>{{Math.round(row.entity[col.field])}}</div>'}
                                       ]};
    $scope.reloadStudentPage = function(student){
        $scope.student = student;
        $scope.students[student._id] = student;
        $scope.current_day = student.days[0];
        // $scope.student_days = [];
        for (var i = 0, len = student.days.length; i < len; i++) {
            student.days[i].day_date = new Date(student.days[i].day).setHours(0,0,0,0);
        }

        if($scope.today !== student.today) {
            $scope.today = student.today;
            $scope.init(function(){$scope.screen = "student";});
        } else {
            $scope.today = student.today;
            $scope.screen = "student";
        }
    };
    $scope.excuse = function(id, day) {
        if (confirm("Excuse " + day + "?")){
            $scope.screen = "saving";
            $http.post('/excuse', {"_id":id, "day": day}).
                success(function(data){
                    $scope.reloadStudentPage(data.student);
                }). error(function(){});
        }
    };

    $scope.saveName = function(name){
        $scope.screen = "loading";
        $http.post('/rename', {"_id":$scope.student._id, "name": name}).
            success(function(data){
                $scope.reloadStudentPage(data.student);
            }). error(function(){});
    };

    $scope.override = function(id, day) {
        if (confirm("Override " + day + "?")){
            $scope.screen = "saving";
            $http.post('/override', {"_id":id, "day": day}).
                success(function(data){
                    $scope.reloadStudentPage(data.student);
                }). error(function(){});
        }
    };
    $scope.requiredMinutes = function(student){
        if (!student) {return "";}
        return student.olderdate ? 330 : 300;
    };
    $scope.createYear = function() {
        if(confirm("Create school year from " + $scope.from_date + " to " + $scope.to_date + "?")){
            $http.post('/year/create', {"from_date":$scope.from_date, "to_date": $scope.to_date}).
                success(function(data){
                    $scope.init();
                }). error(function(){});
        }
    };
    $scope.absent_button_text = function(student){
        if (!student) {return "";}
        return student.absent_today ?  "Remove Absence" :"Mark Absent";
    };
    $scope.toggleAbsent = function(student) {
        if(confirm(student.absent_today ? "Clear today's absence?":"Mark student as absent today?")){
            //student.show_as_absent = !!!student.show_as_absent;
            $scope.screen = "saving";
            $http.post('/student/toggleabsent', {_id : student._id }).
                success(function(data){
                    $scope.reloadStudentPage(data.student);
                }). error(function(){});
        }
    };
    $scope.toggleHours = function(student) {
        if(confirm(student.olderdate ? "Mark student younger?":"Mark student as older starting today?")){
            student.olderdate = !!!student.olderdate;
            $scope.screen = "saving";
            $http.post('/student/togglehours', {_id : student._id }).
                success(function(data){
                    $scope.reloadStudentPage(data.student);
                }). error(function(){});
        }
    };
    $scope.get_missing_swipe = function(){
        var d = new Date();
        if($scope.student.last_swipe_date) {
            d = new Date($scope.student.last_swipe_date + "T10:00:00");
        }
        $scope.missing_direction = ($scope.student.last_swipe_type =="in")?"out":"in";
        if(!$scope.student.in_today
           // && $scope.student.last_swipe_type == "in"
           && $scope.student.direction == "out"){
            $scope.missing_direction = "in";
            d = new Date();
        }
        d.setHours(($scope.missing_direction =="in")?8:15);
        d.setMinutes( 0 );
        $scope.missing_swipe = d;
        $scope.screen = "get-swipe-time";
    };
    $scope.swipe_with_missing = function(missing){
        $scope.student.missing = missing;
        $scope.missing_swipe = "";
        $scope.makeSwipePost();
    };
    $scope.makeSwipePost = function() {
        $scope.screen = "saving";
        $http.post('/swipe', {"_id":$scope.student._id, "direction": $scope.student.direction, "missing": $scope.student.missing}).
            success(function(data){
                $scope.populateStudentsMap(data);
                $scope.showHome($scope.student.name + " swiped successfully!");
            }). error(function(){});
    };
    $scope.inButtonStyle = function(){
        if (!$scope.student){ return "";}
        return ($scope.student.last_swipe_type == "out" || $scope.student.in_today == false) ? "btn-lg btn-success" : "";
    };
    $scope.outButtonStyle = function(){
        if (!$scope.student){ return "";}
        return ($scope.student.last_swipe_type == "in" && $scope.student.in_today) ? "btn-lg btn-success" : "";
    };
    $scope.swipe = function(direction) {
        $scope.student.direction = direction;
        var missing_in = (($scope.student.last_swipe_type == "out"
                           || ($scope.student.last_swipe_type == "in" && !$scope.student.in_today)
                           || !$scope.student.last_swipe_type)
                          && direction == "out"),
            missing_out = ($scope.student.last_swipe_type == "in"
                           && direction == "in");
        if(missing_in || missing_out) {
            $scope.get_missing_swipe();
        } else {
            $scope.makeSwipePost();
        }
    };
    $scope.populateStudentsMap = function(students) {
        if($scope.students[0]) {
            $scope.today = $scope.students[0].today;
        }
        students.map(function(s) {
            $scope.students[s._id] = s;
        });
    };
    $scope.filterStudentsMarkedAbsent = function(students) {
        return $scope.filterStudentsObj(students, function(value){return value['in_today'] === false && value['absent_today'] === true;});
    };
    $scope.filterStudentsNotYetIn = function(students) {
        return $scope.filterStudentsObj(students, function(value){return value['in_today'] === false && value['absent_today'] === false;});
    };
    $scope.filterStudentsIn = function(students) {
        return $scope.filterStudentsObj(students, function(value){
            return value['in_today'] === true && value['last_swipe_type'] === 'in';
        });
    };
    $scope.filterStudentsOut = function(students) {
        return $scope.filterStudentsObj(students, function(value){
            return value['in_today'] === true && value['last_swipe_type'] === 'out';
        });
    };

    $scope.filterStudentsObj = function(students, pred) {
        var result = {};
        angular.forEach(students, function(value, key) {
            if (pred(value)) {
                result[key] = value;
            }
        });
        return result;
    };
    $scope.createStudent = function(name) {
        $scope.screen = "saving";
        $http.post('/student/create', {"name":name}).
            success(function(data){
                $scope.populateStudentsMap(data);
                if(data.made) {
                    $scope.showHome(name + " created successfully!");
                    $scope.message = "";
                    $scope.cstudent = "";
                } else {
                    $scope.screen = "create";
                    $scope.message = "A student named " + name + " already exists";
                }
            }). error(function(){});
    };
    $scope.Math = window.Math;
    $scope.getTotalsStudents = function() {
        $http.post('/student/report', {"year":$scope.current_totals_year}).
            success(function(data){
                $scope.totals_students = data;
                $scope.swipedWorked = false;
                $scope.screen = "student-totals";
                $(window).resize();
            }). error(function(){});
    };
    $scope.getStudents = function(callback) {
        $http.get('/students').
            success(function(data){
                $scope.populateStudentsMap(data);
                if (callback) {callback();}
            }). error(function(){});
    };
    $scope.getYears = function() {
        $http.get('/year/all').
            success(function(data){
                $scope.years = data.years;
                $scope.current_totals_year = data.current_year;
            }). error(function(){});
    };
    $scope.deleteSwipe = function(swipe) {
        if(confirm("Delete swipe?")) {
            $http.post('/swipe/delete', {"swipe":swipe, "_id" : $scope.student._id}).
                success(function(data){
                    $scope.reloadStudentPage(data.student);
                }). error(function(){});
        }
    };
    $scope.deleteYear = function(year) {
        if(confirm("Delete year " + year + "?")){
            $http.post('/year/delete', {"year":year}).
                success(function(data){
                    $scope.years = data.years;
                    $scope.current_totals_year = data.current_year;
                    $scope.init();
                }). error(function(){});
        }
    };
    $scope.editName = function() {
        $http.post('/is-admin').
            success(function(data){
                $scope.isAdmin = true;
            }).error(function(){});
    };
    $scope.isAdmin = false;
    $scope.checkRole = function() {
        $http.post('/is-admin').
            success(function(data){
                $scope.isAdmin = true;
            }).error(function(){});
    };
    $scope.init = function(goTo){
        if (!goTo) {
            goTo = function(){$scope.showHome();};
        }
        $scope.screen = "loading";
        $scope.checkRole();
        $scope.getYears();
        $scope.getStudents(goTo);
    };
    $scope.init();
});
