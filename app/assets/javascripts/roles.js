const Handlebars = require('handlebars');
const utils = require('./utils');

Handlebars.registerHelper('ifEquals', function(arg1, arg2, options) {
    return (arg1 == arg2) ? options.fn(this) : options.inverse(this);
});

export function init(roles, terms) {
    sortAll(roles);
    registerEditorEvents();
    registerTabEvents(roles, terms);
    switchTab(roles, terms, 'Individual');
}

function registerTabEvents(roles, terms) {
    const tabs = document.getElementsByClassName('roles-tab');
    for (let tab of tabs) {
        const roleType = tab.getAttribute('data-type');
        tab.addEventListener('click', () => {
            switchTab(roles, terms, roleType);
        });
    }
}

function switchTab(roles, terms, roleType) {
    const tabs = document.getElementsByClassName('roles-tab');
    for (let tab of tabs) {
        if (tab.getAttribute('data-type') === roleType) {
            tab.classList.add('active');
        }
        else {
            tab.classList.remove('active');
        }
    }
    const filteredRoles = roles.filter(r => r.type === roleType);
    renderIndex(filteredRoles, terms, roleType);
}

function renderIndex(roles, terms, roleType) {
    const container = document.getElementById('roles-index-container');
    if (!roles || !roles.length) {
        container.innerHTML = '<div style="padding:15px;">No roles have been created yet with this type.</div>';
        return;
    }
    const template = Handlebars.compile($('#roles-index-template').html());
    container.innerHTML = template({
        chairHeader: getChairHeader(roleType, terms),
        hasChairs: roles.some(r => getMembers(r, 'Chair').length > 0),
        hasBackups: roles.some(r => getMembers(r, 'Backup').length > 0),
        hasMembers: roles.some(r => getMembers(r, 'Member').length > 0),
        hasEligibilities: roles.some(r => r.eligibility !== 'Anyone'),
        hasNotes: roles.some(r => !!r.notes),
        hasDescriptions: roles.some(r => !!r.description),
        roles: roles.map(role => {
            return {
                id: role.id,
                name: role.name,
                chairs: formatMemberList(role, 'Chair'),
                backups: formatMemberList(role, 'Backup'),
                members: formatMemberList(role, 'Member'),
                eligibility: formatEligibility(role.eligibility),
                notes: role.notes,
                description: role.description
            };
        })
    });
    const editButtons = document.getElementsByClassName('roles-edit-button');
    for (let editButton of editButtons) {
        editButton.addEventListener('click', () => {
            const roleId = editButton.getAttribute('data-id');
            const role = roles.filter(r => r.id == roleId)[0];
            renderEditor(role);
        });
    }
    sorttable.makeSortable(container.querySelector('table'));
}

function renderEditor(role) {
    document.querySelector('.roles-editor').style.display = 'block';
    const table = document.querySelector('.roles-edit-table');
    const template = Handlebars.compile($('#roles-editor-general-template').html());
    const generalHtml = template({
        name: role.name,
        type: role.type,
        eligibility: role.eligibility,
        notes: role.notes,
        description: role.description
    });
    table.innerHTML = generalHtml;
}

function registerEditorEvents() {
    const submitButton = document.getElementById('roles-editor-submit');
    const cancelButton = document.getElementById('roles-editor-cancel');
    submitButton.addEventListener('click', () => {

    });
    cancelButton.addEventListener('click', () => {
        closeEditor();
    });
}

function closeEditor() {
    document.querySelector('.roles-editor').style.display = 'none';
}

function getChairHeader(roleType, terms) {
    if (roleType === 'Individual') {
        return terms.individual;
    }
    return 'Chair';
}

function getMembers(role, memberType) {
    if (!role.records || !role.records.length) {
        return [];
    }
    const lastRecord = role.records[role.records.length - 1];
    if (!lastRecord.members) {
        return [];
    }
    return lastRecord.members.filter(m => m.type === memberType);
}

function formatMemberList(role, memberType) {
    const members = getMembers(role, memberType);
    return members.map(m => getMemberName(m)).filter(m => !!m).join(', ');
}

function formatEligibility(eligibility) {
    if (eligibility === 'StaffOnly') {
        return 'Staff Only';
    }
    if (eligibility === 'StudentOnly') {
        return 'Student Only';
    }
    return '';
}

function getMemberName(member) {
    if (member.person) {
        return member.person.displayName || '';
    }
    return member.personName || '';
}

function sortAll(roles) {
    sortRoles(roles);
    for (let role of roles) {
        if (role.records) {
            sortRecords(role.records);
            for (let record of role.records) {
                if (record.members) {
                    sortMembers(record.members);
                }
            }
        }
    }
}

function sortRoles(roles) {
    sortAlphabetically(roles, r => r.name);
}

function sortRecords(records) {
    // sorts chronologically
    records.sort((a, b) => sortFunction(a.dateCreated, b.dateCreated));
    function sortFunction(a, b) {
        // TODO IMPLEMENT
        return 0;
    }
}

function sortMembers(members) {
    sortAlphabetically(members, m => getMemberName(m));
}

function sortAlphabetically(arr, nameFn) {
    arr.sort((a, b) => {
        const _a = nameFn(a).toLowerCase();
        const _b = nameFn(b).toLowerCase();
        if (_a > _b) return 1;
        if (_a < _b) return -1;
        return 0;
    });
}