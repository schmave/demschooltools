# How to add a new school

1. Add subdomain CNAME record
1. Update Facebook SSO settings ("[App Domains](https://developers.facebook.com/apps/306846672797935/settings/basic/)" and "[Valid OAuth Redirect URIs](https://developers.facebook.com/apps/306846672797935/fb-login/settings/)")
1. Update Google SSO settings ("[Authorized redirect URIs](https://console.cloud.google.com/auth/clients/477883553858.apps.googleusercontent.com?project=api-project-477883553858)")
1. app/controllers/Utils.java: Create a new subclass of OrgConfig. Then register an instance of it in the OrgConfigs static block.
1. Create new entry in organization table (ID, name, short_name):

    ```sql
    INSERT INTO organization(name, shortname) VALUES ('Tall Sudbury School', 'TSS');
    ```

1. Create new entry in organization_hosts table, plus new basic tags:

    ```sql
    INSERT INTO organization_hosts(host, organization_id) VALUES ('foo.demschooltools.com', newId);
    INSERT INTO tag(title, use_student_display, organization_id, show_in_jc) VALUES ('Current Student', true, newId, true);
    INSERT INTO tag(title, use_student_display, organization_id, show_in_jc) VALUES ('Staff', false, newId, true);
    ```

1. Run:

    ```sh
    sudo vim /etc/nginx/sites-enabled/demschooltools # add new host to http and https configs
    sudo service nginx reload
    vim certbot.sh # Add new domain to the list
    sudo ./certbot.sh # Choose Expand at the prompt
    ```

1. Deploy to the server
1. Use DST to add a new user to the new school

# What to test

1. Can login with FB
1. Can login with Google
