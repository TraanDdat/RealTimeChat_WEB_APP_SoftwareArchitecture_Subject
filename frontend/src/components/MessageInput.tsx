import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import useKeyboardSound from "../hooks/useKeyboardSound";
import { useChatStore } from "../store/useChatStore";
import toast from "react-hot-toast";
import { PaperclipIcon, SendIcon, SmileIcon, XIcon } from "lucide-react";

const MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024;
const UPLOAD_TYPES: Record<string, string> = {
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  png: "image/png",
  webp: "image/webp",
  gif: "image/gif",
  pdf: "application/pdf",
  txt: "text/plain",
  doc: "application/msword",
  xls: "application/vnd.ms-excel",
  ppt: "application/vnd.ms-powerpoint",
  docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  xlsx: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  mp4: "video/mp4",
  mov: "video/quicktime",
  webm: "video/webm",
};
const ACCEPTED_FILE_TYPES = Object.values(UPLOAD_TYPES).join(",");
const EMOJIS = [
  "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
  "😊", "🙂", "😉", "😍", "🥰", "😘", "😋", "😎",
  "🤔", "🤗", "😐", "🙄", "😏", "😢", "😭", "😡",
  "🤬", "😱", "🥳", "😴", "🤒", "👍", "👎", "👏",
  "🙌", "🙏", "❤️", "💔", "🔥", "🎉", "✨", "💯",
];

function readAsDataUrl(file: File, mimeType: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result !== "string") {
        reject(new Error("Could not read the selected file"));
        return;
      }
      resolve(`data:${mimeType};base64,${reader.result.split(",", 2)[1]}`);
    };
    reader.onerror = () => reject(new Error("Could not read the selected file"));
    reader.readAsDataURL(file);
  });
}

function MessageInput() {
  const { playRandomKeyStrokeSound } = useKeyboardSound();
  const [text, setText] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedMimeType, setSelectedMimeType] = useState<string | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const textInputRef = useRef<HTMLInputElement | null>(null);
  const emojiButtonRef = useRef<HTMLButtonElement | null>(null);
  const emojiPickerRef = useRef<HTMLDivElement | null>(null);
  const [isEmojiPickerOpen, setIsEmojiPickerOpen] = useState(false);
  const { sendMessage, isSoundEnabled } = useChatStore();

  useEffect(() => {
    if (!isEmojiPickerOpen) return;
    const closeOnOutsideClick = (event: PointerEvent) => {
      const target = event.target;
      if (
        target instanceof Node &&
        !emojiPickerRef.current?.contains(target) &&
        !emojiButtonRef.current?.contains(target)
      ) {
        setIsEmojiPickerOpen(false);
      }
    };
    document.addEventListener("pointerdown", closeOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeOnOutsideClick);
  }, [isEmojiPickerOpen]);

  const insertEmoji = (emoji: string) => {
    const input = textInputRef.current;
    const start = input?.selectionStart ?? text.length;
    const end = input?.selectionEnd ?? text.length;
    const nextText = `${text.slice(0, start)}${emoji}${text.slice(end)}`;
    setText(nextText);
    setIsEmojiPickerOpen(false);
    requestAnimationFrame(() => {
      input?.focus();
      input?.setSelectionRange(start + emoji.length, start + emoji.length);
    });
  };

  const handleSendMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!text.trim() && !selectedFile) return;
    if (isSoundEnabled) playRandomKeyStrokeSound();

    const isImage = selectedMimeType?.startsWith("image/") ?? false;
    let fileDataUrl: string | null = null;
    if (selectedFile && selectedMimeType) {
      try {
        fileDataUrl = await readAsDataUrl(selectedFile, selectedMimeType);
      } catch {
        toast.error("Could not read the selected file");
        return;
      }
    }
    const attachment = !isImage ? fileDataUrl : null;
    sendMessage({
      text: text.trim(),
      image: isImage ? fileDataUrl : null,
      attachment,
      attachmentName: attachment ? selectedFile?.name ?? null : null,
      attachmentMimeType: attachment ? selectedMimeType : null,
    });
    setText("");
    clearSelectedFile();
  };

  const clearSelectedFile = () => {
    if (imagePreview) URL.revokeObjectURL(imagePreview);
    setSelectedFile(null);
    setSelectedMimeType(null);
    setImagePreview(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0];
    if (!file) return;
    if (file.size >= MAX_UPLOAD_SIZE_BYTES) {
      toast.error("File must be smaller than 5 MB");
      event.currentTarget.value = "";
      return;
    }
    const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
    const mimeType = UPLOAD_TYPES[extension];
    if (!mimeType || (file.type && file.type !== mimeType)) {
      toast.error("Choose a supported image, document, or video file");
      event.currentTarget.value = "";
      return;
    }

    setSelectedFile(file);
    setSelectedMimeType(mimeType);
    if (imagePreview) URL.revokeObjectURL(imagePreview);
    setImagePreview(mimeType.startsWith("image/") ? URL.createObjectURL(file) : null);
  };

  return (
    <div className="p-4 border-t border-slate-700/50">
      {selectedFile && (
        <div className="max-w-3xl mx-auto mb-3 flex items-center gap-3">
          <div className="relative">
            {imagePreview ? (
              <img src={imagePreview} alt="Selected image" className="w-20 h-20 object-cover rounded-lg border border-slate-700" />
            ) : (
              <div className="max-w-xs truncate rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-200">
                {selectedFile.name}
              </div>
            )}
            <button
              onClick={clearSelectedFile}
              className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-slate-800 flex items-center justify-center text-slate-200 hover:bg-slate-700"
              type="button"
            >
              <XIcon className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      <form onSubmit={handleSendMessage} className="max-w-3xl mx-auto flex space-x-2 sm:space-x-4">
        <input
          ref={textInputRef}
          type="text"
          maxLength={2000}
          value={text}
          onChange={(event) => {
            setText(event.target.value);
            if (isSoundEnabled) playRandomKeyStrokeSound();
          }}
          onKeyDown={(event) => {
            if (event.key === "Escape") setIsEmojiPickerOpen(false);
          }}
          className="min-w-0 flex-1 bg-slate-800/50 border border-slate-700/50 rounded-lg py-2 px-4"
          placeholder="Type your message..."
        />

        <div className="relative">
          <button
            ref={emojiButtonRef}
            type="button"
            onClick={() => setIsEmojiPickerOpen((open) => !open)}
            className="h-full bg-slate-800/50 text-slate-400 hover:text-slate-200 rounded-lg px-3 transition-colors"
            aria-label="Choose an emoji"
            aria-expanded={isEmojiPickerOpen}
          >
            <SmileIcon className="w-5 h-5" />
          </button>
          {isEmojiPickerOpen && (
            <div
              ref={emojiPickerRef}
              role="dialog"
              aria-label="Choose an emoji"
              className="absolute bottom-full right-0 z-20 mb-2 grid w-64 grid-cols-8 gap-1 rounded-lg border border-slate-700 bg-slate-900 p-2 shadow-xl"
            >
              {EMOJIS.map((emoji) => (
                <button
                  key={emoji}
                  type="button"
                  onClick={() => insertEmoji(emoji)}
                  className="aspect-square rounded-md text-xl hover:bg-slate-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-cyan-400"
                  aria-label={`Insert ${emoji}`}
                >
                  {emoji}
                </button>
              ))}
            </div>
          )}
        </div>

        <input
          type="file"
          accept={ACCEPTED_FILE_TYPES}
          ref={fileInputRef}
          onChange={handleFileChange}
          className="hidden"
        />

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className={`bg-slate-800/50 text-slate-400 hover:text-slate-200 rounded-lg px-4 transition-colors ${
            selectedFile ? "text-cyan-500" : ""
          }`}
          aria-label="Attach a file"
        >
          <PaperclipIcon className="w-5 h-5" />
        </button>
        <button
          type="submit"
          disabled={!text.trim() && !selectedFile}
          className="bg-gradient-to-r from-cyan-500 to-cyan-600 text-white rounded-lg px-4 py-2 font-medium hover:from-cyan-600 hover:to-cyan-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <SendIcon className="w-5 h-5" />
        </button>
      </form>
    </div>
  );
}
export default MessageInput;