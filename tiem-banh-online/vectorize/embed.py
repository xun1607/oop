
import os
from langchain_community.document_loaders import TextLoader
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter

script_dir = os.path.dirname(os.path.abspath(__file__))

file_path_du_lieu = os.path.join(script_dir, "du_lieu.txt")

db_persist_directory = os.path.join(script_dir, "db")

def index():
    print(f"Đang tìm file dữ liệu tại: {file_path_du_lieu}")
    if not os.path.exists(file_path_du_lieu):
        print(f"LỖI: Không tìm thấy file {file_path_du_lieu}. Vui lòng kiểm tra lại đường dẫn, tên file và đảm bảo file đã được lưu.")
        return

    loader = TextLoader(file_path_du_lieu, encoding='utf-8')
    docs = loader.load()
    print(f"Đã tải {len(docs)} document(s) từ {file_path_du_lieu}")

    # CHIA NHỎ TÀI LIỆU
    print("Đang chia nhỏ tài liệu...")
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=500,  # Kích thước mỗi chunk (ký tự)
        chunk_overlap=200, # Số ký tự chồng lấp giữa các chunk
        length_function=len,
        add_start_index=True, # Tùy chọn: thêm chỉ số bắt đầu của chunk trong tài liệu gốc
    )
    texts = text_splitter.split_documents(docs)
    print(f"Đã chia thành {len(texts)} chunks.")
    # KẾT THÚC PHẦN CHIA NHỎ

    print("Đang khởi tạo mô hình embeddings (HuggingFaceEmbeddings)...")
    embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
    
    print(f"Đang tạo/cập nhật Vector DB tại: {db_persist_directory}...")
    db = Chroma.from_documents(
        documents=texts, # SỬ DỤNG 'texts' (các chunks đã chia) THAY VÌ 'docs'
        embedding=embeddings,
        persist_directory=db_persist_directory
    )
    print(f"✅ Vector DB đã được tạo/cập nhật thành công tại: {db_persist_directory}")

if __name__ == "__main__":
    index()