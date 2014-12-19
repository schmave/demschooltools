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
    $http.get('/student/all').
        success(function(data){
            var t = 1;
            $scope.students = data;
        }). error(function(){});
});
