angular.module('app').controller("MainController", function($scope, $http){
    $scope.students = [];
    $scope.screen = "home";
    $scope.showStudent = function(s) {
        $scope.screen = "student";
        $scope.att = s;
        $scope.current_day = s.days[0];
    };
    $scope.showHome = function() {
        $scope.screen = "home";
    };
    $scope.showCreate = function() {
        $scope.screen = "create";
    };
    $scope.showStudents = function() {
        $scope.screen = "student-totals";
    };
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
    $scope.swipe = function(direction, id) {
        $http.post('/swipe', {"_id":id, "direction": direction}).
            success(function(data){
                $scope.att = data;
                $scope.current_day = data.days[0];
            }). error(function(){});
        $scope.getStudents();
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
    $scope.getStudents = function() {
        $http.get('/student/all').
            success(function(data){
                $scope.students = data;
            }). error(function(){});
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
        $scope.screen = "home";
    };
    $scope.init();
});
