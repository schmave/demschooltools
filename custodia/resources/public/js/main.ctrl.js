angular.module('app').controller("MainController", function($scope, $http){
    $scope.students = [];
    $scope.screen = "home";
    $scope.student = null;
    $scope.showStudent = function(s) {
        $scope.screen = "student";
        $scope.student = s;
    };
    $scope.showHome = function() {
        $scope.screen = "home";
    };
    $scope.showCreate = function() {
        $scope.screen = "create";
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
    $http.get('/student/all').
        success(function(data){
            var t = 1;
            $scope.students = data;
        }). error(function(){});
});
