const Handlebars = require('handlebars');

const utils = require('./utils');

const person_template_str =
    '<div class="name"><span class="label label-primary">{{name}}</span>' +
    '<img src="/assets/images/x.png"></div>';

const person_template = Handlebars.compile(person_template_str);

const Person = function(id, name, onClickPerson) {
    this.name = name;
    this.id = id;
    const self = this;

    this.render = function() {
        return person_template({ name });
    };

    // called by PeopleChooser after self.el has been
    // initialized.
    this.activateClick = function() {
        if (onClickPerson) {
            self.el.click(
                function() {
                    onClickPerson(self);
                }
            );
        }
    };
};

const PeopleChooser = function(el, on_add, on_remove, autocomplete_source, onClickPerson) {
    this.el = el;
    this.people = [];
    const self = this;

    this.search_box = el.find(".person_search");
    this.search_box.autocomplete({
        source: autocomplete_source,
        delay: 0,
        autoFocus: true,
    });

    self.one_person_mode = false;

//  el.mouseleave(function() {
//      if (self.people.length > 0) {
//          self.search_box.fadeOut();
//      } } );
//  el.mouseenter(function() { self.search_box.fadeIn(); } );
//
    this.search_box.bind( "autocompleteselect", function(event, ui) {
        const new_person = self.addPerson(ui.item.id, ui.item.label);

        if (on_add && new_person) {
            on_add(new_person);
        }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.addPerson = function(id, name) {
        // Don't add people who have already been added.
        for (const i in self.people) {
            if (id == self.people[i].id) {
                return;
            }
        }

        if (self.one_person_mode) {
            self.search_box.hide();
            self.el.find(".glyphicon").hide();
            utils.selectNextInput(self.search_box);
        }

        const p = new Person(id, name, onClickPerson);
        self.people.push(p);
        p.el = self.el.find(".people").append(p.render()).children(":last-child");
        p.activateClick();

        p.el.find("img").click(function() { self.removePerson(p); });

        return p;
    };

    this.clear = function() {
        while (self.people.length > 0) {
            self.removePerson(self.people[0]);
        }
    };

    this.removePerson = function(person) {
        $(person.el).remove();

        for (const i in self.people) {
            if (self.people[i] == person) {
                self.people.splice(i, 1);
            }
        }

        if (self.one_person_mode) {
            self.search_box.show();
            self.el.find(".glyphicon").show();
        }

        if (on_remove) {
            on_remove(person);
        }
    };

    this.loadPeople = function(people) {
        for (const i in people) {
            self.addPerson(people[i].id, people[i].name);
        }
    };

    this.setOnePersonMode = function(one_person_mode) {
        self.one_person_mode = one_person_mode;
        return self;
    };
};

module.exports = {
    PeopleChooser,
    Person,
}
