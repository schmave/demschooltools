const Handlebars = require('handlebars');

const utils = require('./utils');

const result_template_str =
    '<div class="result" data-id="{{id}}">' +
    '<span class="label label-success">{{name}}</span>' +
    '<img src="/assets/images/x.png">' +
    '</div>';

const result_template = Handlebars.compile(result_template_str);

const Chooser = function (
    el,
    allowMultiple,
    minLength,
    source,
    getLabel,
    onClick,
    onChange,
    onAdd,
    onRemove,
) {
    this.el = el;
    const self = this;

    this.results = [];

    this.search_box = el.find('.search');
    this.search_box.autocomplete({
        autoFocus: true,
        delay: 0,
        minLength,
        source,
    });

    this.search_box.bind('autocompleteselect', function (event, ui) {
        const success = self.addResult(ui.item.id, ui.item.label);

        if (success) {
            if (onAdd) {
                onAdd(ui.item.id);
            }
            if (onChange) {
                onChange();
            }
        }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.addResult = function (id, title, select_next) {
        if (select_next === undefined) {
            select_next = true;
        }
        // Don't add results that have already been added.
        for (const i in self.results) {
            if (id == self.results[i]) {
                return false;
            }
        }

        if (!allowMultiple) {
            self.search_box.hide();
            if (select_next) {
                utils.selectNextInput(self.search_box);
            }
        }

        self.results.push(id);

        const result_el = $(result_template({ name: title, id }));
        self.el.find('.results').append(result_el);

        if (onClick) {
            result_el.find('.label').click(function () {
                onClick(id);
            });
        }

        result_el.find('img').click(function () {
            self.removeResult(result_el);
        });

        return true;
    };

    this.removeResult = function (result_el) {
        $(result_el).remove();

        for (const i in self.results) {
            if (self.results[i] == result_el.data('id')) {
                self.results.splice(i, 1);
            }
        }

        if (!allowMultiple) {
            self.search_box.show();
        }

        if (onRemove) {
            onRemove(result_el.data('id'));
        }
        if (onChange) {
            onChange();
        }
    };

    this.clear = function () {
        self.results = [];
    };

    this.loadData = function (json) {
        self.clear();
        self.el.find('.results').html('');
        if (allowMultiple) {
            for (const i in json) {
                self.addResult(json[i].id, getLabel(json[i]), false);
            }
        } else if (json) {
            self.addResult(json.id, getLabel(json), false);
        }
    };
};

module.exports = {
    Chooser,
};
