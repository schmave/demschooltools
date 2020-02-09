from locust import HttpLocust, TaskSet, task, between
import random

HEADERS = {
    'Cookie': 'PLAY_SESSION=81d7522b35644dcda076298df26cb844a5a4b9d1-pa.u.exp=1581281301529&pa.p.id=google&pa.s.id=8128eb62-0afb-433a-b2bd-92e80fa0c3a3&pa.u.id=105655990568065512213',
}

class UserBehaviour(TaskSet):
    def on_start(self):
        """ on_start is called when a Locust start before any task is scheduled """
        pass

    def on_stop(self):
        """ on_stop is called when the TaskSet is stopping """
        pass

    @task(1)
    def index(self):
        self.client.get(f"/", headers=HEADERS)

    @task(5)
    def refs(self):
        self.client.get(f"/getCaseReferencesJson?case_id=44604", headers=HEADERS)

class WebsiteUser(HttpLocust):
    task_set = UserBehaviour
    wait_time = between(0.05, 0.2)
