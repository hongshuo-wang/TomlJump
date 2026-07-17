from dataclasses import dataclass


@dataclass
class PythonServiceConfig:
    python_token: str
    schema: str
