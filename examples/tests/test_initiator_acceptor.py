import pytest
import threading
import subprocess


class Acceptor(threading.Thread):
    def _init__(self):
        threading.Thread.__init__(self)
        self.name = "Acceptor"

    def run(self):
        print("Starting " + self.name)
        out = subprocess.check_output(
            ["java", "-jar", "../acceptor/philadelphia-acceptor.jar", "4000"])
        print(out)


@pytest.fixture
def start_acceptor():
    acceptor = Acceptor()
    acceptor.start()
    acceptor


def test_answer(start_acceptor, capsys):
    out = subprocess.check_output([
        "java", "-jar", "../initiator/philadelphia-initiator.jar", "localhost",
        "4000", "50000", "10000"
    ],
                                  stderr=subprocess.STDOUT)
    print(out)
    assert out != ""
