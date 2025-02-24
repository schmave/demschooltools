# How to add a new school

-   Add subdomain CNAME record
-   Update Facebook SSO settings ("[App Domains](https://developers.facebook.com/apps/306846672797935/settings/basic/)" and "[Valid OAuth Redirect URIs](https://developers.facebook.com/apps/306846672797935/fb-login/settings/)")
-   Update Google SSO settings ("[Authorized redirect URIs](https://console.cloud.google.com/auth/clients/477883553858.apps.googleusercontent.com?project=api-project-477883553858)")
-   app/controllers/Utils.java: Create a new subclass of OrgConfig. Then register an instance of it in the OrgConfigs static block.
-   Create new entry in organization table (ID, name, short_name)
-   Create new entry in organization_hosts table
-   Run:

        sudo vim /etc/nginx/sites-enabled/demschooltools # add new host to http and https configs
        sudo service nginx reload
        vim certbot.sh # Add new domain to the list
        sudo ./certbot.sh # Choose Expand at the prompt

-   Deploy to the server
-   Use DST to add a new user to the new school

# What to test

-   Can login with FB
-   Can login with Google
