var React = require('react'),
    Skylight = require('react-skylight');

module.exports = React.createClass({
    show: function () {
        this.refs.modal.show();
        $(document.body).off('keydown');
        $(document.body).on('keydown', this.handleKeyDown);
    },
    hide: function () {
        this.refs.modal.hide();
    },
    handleKeyDown: function (keypress) {
        if (keypress.keyCode == 27 /*esc*/) {
            this.hide();
            this.unbindEsc();
        }
    },
    unbindEsc: function(){
        $(document.body).off('keydown');
    },
    componentWillUnMount: function () {
        this.unbindEsc();
    },
    render: function () {
        return <Skylight ref="modal" dialogStyles={{height: '450px', backgroundColor: '', boxShadow: ''}}>
            <div className="inner-large-content panel panel-primary" style={{height: '100%'}}>
                <div className="panel-heading">{this.props.title}</div>
                <div className="panel-body">
                    {this.props.children}
                </div>
            </div>
        </Skylight>;
    }
});

