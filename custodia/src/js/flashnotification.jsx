var React = require('react'),
    store = require('./flashnotificationstore'),
    constants = require('./appconstants');

var exports = React.createClass({
    componentDidMount: function(){
        store.addChangeListener(this._onChange);
    },
    _onChange: function(){
        var message = store.getLatest();
        if(message){
            this.setState({showing: true, message: message});
        }
        window.setTimeout(function(){
            this.setState({showing: false});
        }.bind(this), 3000);
    },
    getInitialState: function(){
        return {showing: false};
    },
    render: function(){
        if(this.state.showing){
            return <div className="alert alert-success collapsable ng-binding" role="alert" ng-show="swipedWorked">
                {this.state.message}
            </div>;
        }else{
            return <span style={{display:'none'}}></span>;
        }
    }
});

module.exports = exports;
