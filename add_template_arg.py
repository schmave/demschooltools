import os
import sys

def add_org_config_to_file(filename, template_name):
    existing_content = None
    with open(filename) as f:
        existing_content = f.read()

    lines = existing_content.splitlines()

    needs_save = False

    if filename.endswith('.java'):
        search_string = f'{template_name}.render('

        for i, line in enumerate(lines):
            if search_string in line and 'OrgConfig' not in line:
                needs_save = True
                lines[i] = line.replace(search_string,
                    f'{search_string}OrgConfig.get(Organization.getByHost(request)), ')
    elif f'/{template_name}.scala.html' in filename:
        search_string = '@('
        assert search_string in lines[0], filename
        if 'orgConfig' not in lines[0]:
            needs_save = True
            lines[0] = lines[0].replace('@(', '@(orgConfig: OrgConfig, ')

        for i, line in enumerate(lines):
            search_string = 'OrgConfig.get()'
            if search_string in line:
                lines[i] = line.replace(search_string, 'orgConfig')
                needs_save = True
    else:
        if 'orgConfig' in existing_content and 'orgConfig: OrgConfig' not in existing_content:
            print(filename, 'needs orgConfig argument')

        search_string = f'@{template_name}('
        for i, line in enumerate(lines):
            if search_string in line and 'orgConfig' not in line:
                needs_save = True
                lines[i] = line.replace(
                    search_string, f'{search_string}orgConfig, ')

    if needs_save:
        with open(filename, 'w') as f:
            f.write('\n'.join(lines))


def add_current_user_to_file(filename, template_name):
    existing_content = None
    with open(filename) as f:
        existing_content = f.read()

    lines = existing_content.splitlines()

    needs_save = False

    if filename.endswith('.java'):
        search_string = f'{template_name}.render('

        for i, line in enumerate(lines):
            if search_string in line and 'currentUsername' not in line:
                needs_save = True
                lines[i] = line.replace(search_string,
                                        f'{search_string}Application.currentUsername(request), ')
    elif f'/{template_name}.scala.html' in filename:
        search_string = '@('
        assert search_string in lines[0], filename
        if 'currentUsername' not in lines[0]:
            needs_save = True
            lines[0] = lines[0].replace('@(', '@(currentUsername: Optional[String], ')

        for i, line in enumerate(lines):
            search_string = 'Application.currentUsername()'
            if search_string in line:
                lines[i] = line.replace(search_string, 'currentUsername')
                needs_save = True
    else:
        if 'currentUsername' in existing_content and 'currentUsername: Optional[String]' not in existing_content:
            print(filename, 'needs currentUsername argument')

        search_string = f'@{template_name}('
        for i, line in enumerate(lines):
            if search_string in line and 'currentUsername' not in line:
                needs_save = True
                lines[i] = line.replace(
                    search_string, f'{search_string}currentUsername, ')

    if needs_save:
        with open(filename, 'w') as f:
            f.write('\n'.join(lines))


def add_request_to_file(filename, template_name):
    existing_content = None
    with open(filename) as f:
        existing_content = f.read()

    lines = existing_content.splitlines()

    needs_save = False

    if filename.endswith('.java'):
        pass
        # Use IntelliJ structural replace for this
        # in_call = False
        # for i, line in enumerate(lines):
        #     if f'{template_name}.render(' in line:
        #         in_call = True
        #     if in_call and ';' in line:
        #         assert '));' in line, f'{filename} - {line}'
        #         needs_save = True
        #         in_call = False
        #         lines[i] = line.replace('));', ', request));')

    elif f'.scala.html' in filename:
        search_string = '@('
        assert search_string in lines[0], filename

        for i, line in enumerate(lines):
            if ')' in line:
                if 'implicit' not in line:
                    last_paren_i = line.rindex(')')
                    lines[i] = line[:last_paren_i] + ')(implicit request: Request)'
                    needs_save = True
                break

    if needs_save:
        with open(filename, 'w') as f:
            f.write('\n'.join(lines))

def modify_file(template_name):
    for root, unused_dirs, files in os.walk('app'):
        for filename in files:
            if filename.endswith('.java') or filename.endswith('.scala.html'):
                add_request_to_file(os.path.join(root, filename), template_name)

def main():
    for arg in sys.argv[1:]:
        modify_file(arg)

if __name__ == '__main__':
    main()
