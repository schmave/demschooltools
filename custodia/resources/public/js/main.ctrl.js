angular.module('app').controller("MainController", function(){
    var vm = this;
    vm.students = [];
    $http.get('/student/all').
        success(function(data){
        vm.students = data;
        }). error(function(){});
});
