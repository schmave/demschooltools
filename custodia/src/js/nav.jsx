var react = require('react');

module.exports = reach.createClass({
    render: function () {
        return <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div class="container">
                <div class="navbar-header">
                    <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar"
                            aria-expanded="false" aria-controls="navbar">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <a class="navbar-brand" href="/">Overseer</a>
                </div>
                <div id="navbar" class="collapse navbar-collapse">
                    <ul class="nav navbar-nav">
                        <li class="{{(screen==='home')?'active':''}}"><a href="#" ng-click="showHome()">Home</a></li>
                        <li class="{{(screen==='student-totals')?'active':''}}"><a href="#" ng-show="isAdmin"
                                                                                   ng-click="showStudents()">Student
                            Totals</a></li>
                        <li><a href="/logout">Logout</a></li>
                    </ul>
                </div>
                <!--/.nav-collapse -->
            </div>
        </nav>;
    }
});
