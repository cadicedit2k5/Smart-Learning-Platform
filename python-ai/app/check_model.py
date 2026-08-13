from google import genai

from app.configs.config import get_settings

settings = get_settings()

client = genai.Client(
    api_key=settings.google_api_key.get_secret_value()
)

print("Các model dùng để chat/generate:")

for model in client.models.list():
    if "generateContent" in model.supported_actions:
        print(model.name)