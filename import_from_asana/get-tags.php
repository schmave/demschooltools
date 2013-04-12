<?php
require_once("asana.php");

// See class comments and Asana API for full info

$asana = new Asana("fenQa8R.QxlDhldrXvZfMSL640uYo8Vl"); // Your API Key, you can get it in Asana
$projectId = '865181772415'; // Your Project ID Key, you can get it in Asana
$workspaceId = '865181681187';

$result = json_decode($asana->getWorkspaceTags($workspaceId));
$all_tags = $result->data;

print "<p>tags = [ ";
foreach ($all_tags as $tag) {
    print '"' . $tag->name . "\", ";
}
print "]";

print "<p>tags_to_tasks = {";

foreach ($all_tags as $tag) {
    print '"' . $tag->name . "\": ";

    $result = json_decode($asana->getTasksWithTag($tag->id));

	print "[";
    foreach ($result->data as $person) {
        $person_result = json_decode($asana->getTask($person->id));
        $person = $person_result->data;
        if (count($person->projects) >= 1 && $person->projects[0]->id == 865181772415) {
			print "\"$person->name\", ";
        }
    }
	print "], \n";
}

