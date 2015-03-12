angular.module('app', ['ui.bootstrap']);
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
    $scope.showStudent = function(s, id) {
        $scope.screen = "loading";
        $scope.swipedWorked = false;
        // update the student with the fresh day
        $scope.getStudent(id, function(s){
            $scope.backStudent();
            $scope.student = s;
            $scope.studenti = id;
            $scope.current_day = s.days[0];
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
        $scope.getStudents(function(){
            $scope.swipedWorked = false;
            $scope.screen = "student-totals";
        });
    };
    $scope.setDay = function(s) {
        $scope.current_day = s;
    };
    $scope.reloadStudentPage = function(data){
        $scope.student = data.student;
        $scope.students[data.student._id] = data.student;
        $scope.current_day = data.student.days[0];
        // $scope.loadStudentData(data.all);
        $scope.screen = "student";
    };
    $scope.excuse = function(id, day) {
        if (confirm("Excuse " + day + "?")){
            $scope.screen = "saving";
            $http.post('/excuse', {"_id":id, "day": day}).
                success(function(data){
                    $scope.reloadStudentPage(data);
                }). error(function(){});
        }
    };
    $scope.override = function(id, day) {
        if (confirm("Override " + day + "?")){
            $scope.screen = "saving";
            $http.post('/override', {"_id":id, "day": day}).
                success(function(data){
                    $scope.reloadStudentPage(data);
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
    $scope.toggleHours = function(student) {
        if(confirm(student.olderdate ? "Mark student younger?":"Mark student as older starting today?")){
            student.olderdate = !!!student.olderdate;
            $http.post('/student/togglehours', {_id : student._id }).
                success(function(data){
                    $scope.getStudents();
                    $scope.init();
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
           && $scope.student.last_swipe_type == "in"
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
                // $scope.loadStudentData(data);
                $scope.student = data.student;
                $scope.students[data.student._id] = data.student;
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
        students.map(function(s) {
            $scope.students[s._id] = s;
        });
    };
    $scope.filterStudentsNotYetIn = function(students) {
        return $scope.filterStudentsObj(students, function(value){return value['in_today'] === false;});
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
                $scope.populateStudentsMap(data.students);
                
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
    $scope.getTotalsStudents = function() {
        $http.post('/student/all', {"year":$scope.current_totals_year}).
            success(function(data){
                $scope.totals_students = data;
            }). error(function(){});
    };
    $scope.loadStudentData = function(data){
        $scope.populateStudentsMap(data);
        $scope.totals_students = data;
    };
    $scope.getStudents = function(callback) {
        $http.post('/student/all').
            success(function(data){
                $scope.loadStudentData(data);
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
                    $scope.reloadStudentPage(data);
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
    $scope.isAdmin = false;
    $scope.checkRole = function() {
        $http.post('/is-admin').
            success(function(data){
                $scope.isAdmin = true;
            }).error(function(){});
    };
    $scope.init = function(){
        $scope.screen = "loading";
        $scope.checkRole();
        $scope.getYears();
        $scope.getStudents(function(){
            $scope.showHome();
        });
    };
    $scope.init();
});
