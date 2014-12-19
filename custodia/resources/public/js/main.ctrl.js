angular.module('app').controller("MainController", function($scope, $http){
    // var vm = this;
    $scope.students = [];
    $http.get('/student/all').
        success(function(data){
            var t = 1;
            $scope.students = data;
        }). error(function(){});
});
