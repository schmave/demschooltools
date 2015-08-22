var React = require('react'),
    Skylight = require('react-skylight');

module.exports = React.createClass({
    show: function () {
        this.refs.modal.show();
    },
    hide: function(){
        this.refs.modal.hide();
    },
    render: function () {
        return <Skylight ref="modal">
            <div className="panel panel-primary" style={{height: '95%'}}>
                <div className="panel-heading">{this.props.title}</div>
                <div className="panel-body">
                    {this.props.children}
                </div>
            </div>
        </Skylight>;
    }
});

