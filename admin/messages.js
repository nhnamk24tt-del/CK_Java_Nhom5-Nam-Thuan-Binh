const apiBase = window.location.origin.replace(/:\d+$/,'') + ':3000/api';

const activeAdmin = { activeUser: { role: 'admin' } };

async function fetchMessages(){
  const res = await fetch(apiBase + '/messages/list', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(activeAdmin)
  });
  return res.json();
}

async function fetchMessage(id){
  const res = await fetch(apiBase + '/messages/' + id, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(activeAdmin)
  });
  return res.json();
}

async function sendReply(id, text){
  const res = await fetch(apiBase + '/messages/' + id + '/reply', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...activeAdmin, response_text: text })
  });
  return res.json();
}

async function markRead(id){
  const res = await fetch(apiBase + '/messages/' + id + '/mark-read', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(activeAdmin)
  });
  return res.json();
}

function renderList(items){
  const tbody = document.querySelector('#messagesTable tbody');
  tbody.innerHTML = '';
  items.forEach(it => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${it.id}</td>
      <td>${it.sender_name || ''}<br/><small>${it.sender_email||''}</small></td>
      <td>${it.subject}</td>
      <td>${new Date(it.created_at).toLocaleString()}</td>
      <td>${it.status}</td>
      <td class="actions">
        <button data-id="${it.id}" class="viewBtn">View</button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  document.querySelectorAll('.viewBtn').forEach(b=>b.addEventListener('click', ()=>openDetail(b.dataset.id)));
}

async function openDetail(id){
  const resp = await fetchMessage(id);
  if(!resp.success){ alert(resp.message||'Error'); return; }
  const m = resp.data;
  const container = document.getElementById('messageDetail');
  container.innerHTML = `
    <h3>Message #${m.id}</h3>
    <p><strong>From:</strong> ${m.sender_name} &lt;${m.sender_email}&gt;</p>
    <p><strong>Phone:</strong> ${m.sender_phone || '-'}</p>
    <p><strong>Subject:</strong> ${m.subject}</p>
    <hr/>
    <p>${m.message}</p>
    <hr/>
    <p><strong>Admin Reply:</strong></p>
    <p>${m.admin_reply || '<em>No reply yet</em>'}</p>
  `;

  document.getElementById('replyText').value = '';
  document.getElementById('detailModal').classList.remove('hidden');

  const sendBtn = document.getElementById('sendReplyBtn');
  sendBtn.onclick = async () => {
    const text = document.getElementById('replyText').value.trim();
    if(!text) return alert('Please type a reply');
    const r = await sendReply(id, text);
    if(r.success){ alert('Reply saved'); closeModal(); load(); } else alert(r.message||'Error');
  };

  document.getElementById('markReadBtn').onclick = async ()=>{
    const r = await markRead(id);
    if(r.success){ alert('Marked read'); closeModal(); load(); } else alert(r.message||'Error');
  };
}

function closeModal(){ document.getElementById('detailModal').classList.add('hidden'); }

async function load(){
  const res = await fetchMessages();
  if(res.success){ renderList(res.data); } else { alert(res.message||'Failed to load'); }
}

document.getElementById('refreshBtn').addEventListener('click', load);
document.getElementById('closeModal').addEventListener('click', closeModal);

load();
