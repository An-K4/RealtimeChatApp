import { config } from "../config/index.js";

/* =======================
   LẤY PARAMS TỪ URL
======================= */
const params = new URLSearchParams(window.location.search);

const friendId = params.get("to");     // người gọi
const fromId   = params.get("from");   // người nhận
const token    = params.get("token");  // 🔥 token từ Desktop

if (!token) {
    alert("Không có token xác thực, vui lòng đăng nhập lại.");
    throw new Error("Missing auth token");
}

/* =======================
   SOCKET.IO (AUTH OK)
======================= */
const socket = io(config.socketUrl, {
    auth: { token }
});

/* =======================
   WEBRTC
======================= */
const pc = new RTCPeerConnection({
    iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
});

const localVideo  = document.getElementById("localVideo");
const remoteVideo = document.getElementById("remoteVideo");

/* =======================
   CAMERA + MIC
======================= */
navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    .then(stream => {
        localVideo.srcObject = stream;
        stream.getTracks().forEach(track => pc.addTrack(track, stream));
    })
    .catch(err => {
        console.error("Camera/Mic error:", err);
        alert("Không thể truy cập camera/micro.");
    });

/* =======================
   NHẬN STREAM
======================= */
pc.ontrack = e => {
    remoteVideo.srcObject = e.streams[0];
};

/* =======================
   XÁC ĐỊNH NGƯỜI KIA
======================= */
const otherUserId = friendId || fromId;

/* =======================
   ICE CANDIDATE
======================= */
pc.onicecandidate = e => {
    if (e.candidate && otherUserId) {
        socket.emit("call:ice", {
            to: otherUserId,
            candidate: e.candidate
        });
    }
};

/* =======================
   NGƯỜI GỌI → OFFER
======================= */
async function startCall() {
    if (!friendId) return;

    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);

    socket.emit("call:offer", {
        to: friendId,
        offer
    });
}

/* =======================
   NGƯỜI NHẬN → ANSWER
======================= */
async function answerCall(offer) {
    if (!fromId) return;

    await pc.setRemoteDescription(offer);
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);

    socket.emit("call:answer", {
        to: fromId,
        answer
    });
}

/* =======================
   FLOW
======================= */
if (friendId) {
    startCall();
}

if (fromId) {
    socket.on("call:offer", async data => {
        if (data.from === fromId) {
            await answerCall(data.offer);
        }
    });
}

socket.on("call:answer", async data => {
    if (data.from === friendId) {
        await pc.setRemoteDescription(data.answer);
    }
});

socket.on("call:ice", async data => {
    if (data.candidate && (data.from === friendId || data.from === fromId)) {
        await pc.addIceCandidate(data.candidate);
    }
});

/* =======================
   KẾT THÚC CALL
======================= */
socket.on("call:end", data => {
    if (data.from === otherUserId) {
        window.close();
    }
});

document.getElementById("endCallBtn").onclick = () => {
    if (otherUserId) {
        socket.emit("call:end", { to: otherUserId });
    }
    window.close();
};
