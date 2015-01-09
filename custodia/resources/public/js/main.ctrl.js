angular.module('app', ['ui.bootstrap']);
angular.module('app').controller("MainController", function($scope, $http){
    $scope.students = [];
    $scope.screen = "home";
    $scope.current_totals_year = null;
    $scope.missing_date = "";
    $scope.missing_swipe = "";
    $scope.backStudent = function(s) {
        $scope.screen = "student";
    };
    $scope.showStudent = function(s) {
        $scope.backStudent();
        $scope.att = s;
        $scope.current_day = s.days[0];
    };
    $scope.showHome = function() {$scope.screen = "home";};
    $scope.showSwipedToday = function() {$scope.screen = "swiped-today";};
    $scope.showCreate = function() {$scope.screen = "create";};
    $scope.showCreateYear = function() {$scope.screen = "create-year";};
    $scope.showStudents = function() {$scope.screen = "student-totals";};
    $scope.setDay = function(s) {
        $scope.current_day = s;
    };
    $scope.override = function(id, day) {
        if (confirm("Override " + day + "?")){
            $http.post('/override', {"_id":id, "day": day}).
                success(function(data){
                    $scope.att = data;
                    $scope.current_day = data.days[0];
                }). error(function(){});
            $scope.getStudents();
        }
    };
    $scope.createYear = function() {
        if(confirm("Create school year from " + $scope.from_date + " to " + $scope.to_date + "?")){
            $http.post('/year/create', {"from_date":$scope.from_date, "to_date": $scope.to_date}).
                success(function(data){
                    $scope.init();
                }). error(function(){});
        }
    };
    $scope.get_missing_swipe = function(){
        var d = new Date();
        $scope.missing_direction = ($scope.att.last_swipe_type =="in")?"out":"in";
        d.setHours(($scope.missing_direction =="in")?8:15);
        d.setMinutes( 0 );
        $scope.missing_swipe = d;
        $scope.screen = "get-swipe-time";
    };
    $scope.swipe_with_missing = function(missing){
        $scope.att.missing = missing;
        $scope.missing_swipe = "";
        $scope.makeSwipePost();
    };
    $scope.makeSwipePost = function() {
        $http.post('/swipe', {"_id":$scope.att._id, "direction": $scope.att.direction, "missing": $scope.att.missing}).
            success(function(data){
                $scope.att = data;
                $scope.current_day = data.days[0];
                $scope.getStudents();
                $scope.showHome();
            }). error(function(){});
    };
    $scope.hideSwipeOut = function(){
        if ($scope.att) {
            return ($scope.att.today != $scope.att.last_swipe_date) && $scope.att.last_swipe_type;
        }
        return true;
    };
    $scope.swipe = function(direction) {
        $scope.att.direction = direction;
        if((($scope.att.last_swipe_type == "out" || !$scope.att.last_swipe_type) && direction == "out")
           || ($scope.att.last_swipe_type == "in" && direction == "in")) {
            $scope.get_missing_swipe(); 
        } else {
            $scope.makeSwipePost();
        }
    };
    $scope.createStudent = function(name) {
        $http.post('/student/create', {"name":name}).
            success(function(data){
                $scope.students = data.students;
                if(data.made) {
                    $scope.screen = "home";
                    $scope.message = "";
                    $scope.cstudent = "";
                } else {
                    $scope.message = "A student with that name already exists";
                }
            }). error(function(){});
    };
    $scope.getTotalsStudents = function() {
        $http.post('/student/all', {"year":$scope.current_totals_year}).
            success(function(data){
                $scope.totals_students = data;
            }). error(function(){});
    };
    $scope.getStudents = function() {
        $http.post('/student/all').
            success(function(data){
                $scope.students = data;
                $scope.totals_students = data;
            }). error(function(){});
    };
    $scope.getYears = function() {
        $http.get('/year/all').
            success(function(data){
                $scope.years = data.years;
                $scope.current_totals_year = data.current_year;
            }). error(function(){});
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
    $scope.isAdmin = false;
    $scope.checkRole = function() {
        $http.post('/is-admin').
            success(function(data){
                $scope.isAdmin = true;
            }).error(function(){});
    };
    $scope.init = function(){
        $scope.checkRole();
        $scope.getStudents();
        $scope.getYears();
    };
    $scope.init();
});
