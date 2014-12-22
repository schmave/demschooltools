angular.module('app').controller("MainController", function($scope, $http){
    $scope.students = [];
    $scope.screen = "home";
    $scope.student = null;
    $scope.showStudent = function(s) {
        $scope.screen = "student";
        $scope.student = s;
        $http.get('/swipe/' + s._id)
            .success(function(data){$scope.swipes = data;})
            .error(function(){});
    };
    $scope.showHome = function() {
        $scope.screen = "home";
    };
    $scope.showCreate = function() {
        $scope.screen = "create";
    };
    $scope.swipeOut = function() {
        $scope.screen = "create";
    };
    $scope.swipe = function(direction, id) {
        $http.post('/swipe', {"_id":id, "direction": direction}).
            success(function(data){
                $scope.swipes = data.swipes;
            }). error(function(){});
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
