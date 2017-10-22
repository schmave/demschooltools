var Handlebars = require('handlebars');

var utils = require('./utils');

var person_template_str =
    '<div class="name"><span class="label label-primary">{{name}}</span>' +
    '<img src="/assets/images/x.png"></div>';

var person_template = Handlebars.compile(person_template_str);

var Person = function(id, name) {
    this.name = name;
    this.id = id;
    var self = this;

    this.render = function() {
        return person_template({"name": name});
    };

    // called by PeopleChooser after self.el has been
    // initialized.
    this.activateClick = function() {
        self.el.click(
            function() {
                showPersonHistoryInSidebar(self);
            }
        );
    };
};

var PeopleChooser = function(el, on_add, on_remove, autocomplete_source) {
    this.el = el;
    this.people = [];
    var self = this;

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
        new_person = self.addPerson(ui.item.id, ui.item.label);

        if (on_add && new_person) {
            on_add(new_person);
        }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.addPerson = function(id, name) {

        // Don't add people who have already been added.
        for (var i in self.people) {
            if (id == self.people[i].id) {
                return;
            }
        }

        if (self.one_person_mode) {
            self.search_box.hide();
            self.el.find(".glyphicon").hide();
            utils.selectNextInput(self.search_box);
        }

        var p = new Person(id, name);
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

        for (var i in self.people) {
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
        for (var i in people) {
            self.addPerson(people[i].id, people[i].name);
        }
    };

    this.setOnePersonMode = function(one_person_mode) {
        self.one_person_mode = one_person_mode;
        return self;
    };
};

module.exports = {
    PeopleChooser: PeopleChooser,
    Person: Person,
}
