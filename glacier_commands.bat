C:\Python27\Scripts\aws glacier initiate-job --account-id - ^
    --vault-name demschooltools-backups ^
    --job-parameters "{\"Type\": \"inventory-retrieval\"}"


rem request an archive
rem C:\Python27\Scripts\aws glacier initiate-job --account-id - ^
rem     --vault-name demschooltools-backups ^
rem     --job-parameters file://args.json

rem put this in a file to request an archive
rem {
rem      "Type": "archive-retrieval",
rem      "ArchiveId": "...",
rem      "Description": "6/14"
rem }


rem get finished archive retrieval or inventory
rem C:\Python27\Scripts\aws glacier get-job-output --account-id - ^
rem     --vault-name demschooltools-backups ^
rem     --job-id Ip0zLTco2QAzzxlVZSVxJaCEHDmw8ozVwR_iFKE3xkaN79CXDSZ8gCihliK_N25IhsEN2U2QLzj1Bo_ttzxze1rv_voR ^
rem     june_14.zip


rem show jobs that are in progress
rem C:\Python27\Scripts\aws glacier list-jobs ^
rem     --account-id - --vault-name demschooltools-backups
