from pymongo import MongoClient
# uri = "mongodb+srv://huynhhuuhaitrieu_db_user:50MbNegzYFRm2DDw@cluster0.ubv1k1i.mongodb.net/"
uri = "mongodb+srv://huynhhuuhaitrieu_db_user:50MbNegzYFRm2DDw@cluster0.ubv1k1i.mongodb.net/"
client = MongoClient(uri)

db = client.todo_db
collection = db["todo_data"]

# try:
#     client.admin.command("ping")
#     print("Connected successfully")
#     client.close()

# except Exception as e:
#     raise Exception(
#         "The following error occurred: ", e)