function doHSLogin(onOk=null,otherwise=null) {
	var params = new URLSearchParams(document.location.search);
	
	var access_token = params.get('access_token');
	var expires_in = params.get('expires_in');
	var username = params.get('username');
	
	if(access_token != null && expires_in != null && username != null) {
		//app: 'mirafun',
		//callbackURL: 'https://localhost:8443/callback',
		//	scope: ['login']
		let api = new HS.Client({
			accessToken: access_token
		});

		api.me(function(err, res) {
			console.log("res");
		    console.log(res);
		    console.log("err");
		    console.log(err);
		    if(res) {
		    	var obj = res;
		    	res.accessToken = access_token;
		    	res.username = username;
		    	res.expires_in = new Date().getTime()+expires_in;
		    	window.localStorage.setItem("hslogin", JSON.stringify(obj));
		    	if(onOk != null) onOk(res);
		    }
		    else {
		    	if(otherwise != null) otherwise(err);
		    }
		});
	}
	else { 
		if(otherwise != null) otherwise(null);
	}
}
var HS_LOGIN_OBJECT = null;
function hsLoginObj() {
	if(HS_LOGIN_OBJECT === null) {
		try {
			var hslogin = window.localStorage.getItem("hslogin");
			if(hslogin != null) {
				var obj = JSON.parse(hslogin);
				if(new Date().getTime() < obj.expires_in) {
					HS_LOGIN_OBJECT = obj;
					return obj;
				}
				else {
					window.localStorage.clear("hslogin");
				}
			}
			HS_LOGIN_OBJECT = false;
		}
		catch(e) {
			console.log(e);
			HS_LOGIN_OBJECT = false;
		}
	}
	return HS_LOGIN_OBJECT;
}
function hsLogOut() {
	window.localStorage.clear("hslogin");
	window.location.replace("/");
}
function isLoggedIn() {
	return hsLoginObj() !== false;
}
function username() {
	if(!isLoggedIn()) return "";
	return hsLoginObj().username;
}
function username() {
	if(!isLoggedIn()) return null;
	return hsLoginObj().username;
}
function profileimg() {
	if(!isLoggedIn()) return null;
	return hsLoginObj().user_metadata.profile.profile_image;
}

//
function sd(method, action, data, sdHandle, eHandle) {
	if(typeof data != 'string') data = encodeParams(data);
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.responseType = "json";
	xmlHttp.onreadystatechange = function() { 
		if(xmlHttp.readyState == XMLHttpRequest.DONE) {
			if(xmlHttp.status == 200) {
				sdHandle(xmlHttp);
			}
			else {
				eHandle(xmlHttp, xmlHttp.status);
			}
		}
	};
	xmlHttp.addEventListener( 'error', function(e) {
		console.log("error sd " + e);
		eHandle(xmlHttp, e);
	});
	xmlHttp.open(method, action, true);
	xmlHttp.send(data);
}
function encodeParams(data) {
	let urlEncodedData = "",
	  urlEncodedDataPairs = [],
	  name;

	for( name in data ) {
		urlEncodedDataPairs.push( encodeURIComponent(name) + '=' + encodeURIComponent(data[name]));
	}
	urlEncodedData = urlEncodedDataPairs.join( '&' ).replace( /%20/g, '+' );
	return urlEncodedData;
}
//
function initLogin() {
	var b = document.getElementById("loginButton");
	if(isLoggedIn()) {
		b.innerHTML = "Logout";
		b.href="#";
		b.onclick = hsLogOut;
	}
	else {
		b.innerHTML = "Login/Register";
		if(window.location.toString().startsWith("https://localhost:8443")) {
			b.href="https://hivesigner.com/import?redirect_uri=https%3A%2F%2Flocalhost:8443%2Fcallback&scope=login&clientId=hivereblogger";
		}
		else b.href="https://hivesigner.com/import?redirect_uri=https%3A%2F%2Fhivereblogger.com%2Fcallback&scope=login&clientId=hivereblogger";
		b.onclick = null;
	}
}
var glTasks = null;
var glForm = {};
function listTasks() {
	var obj = {"reblogAccount": username() };

	postFormTask(obj)
}
function refreshTaskList() {
	var table = document.getElementById("ta");
	table.innerHTML = "";
	
	if(glTasks == null) return;
	for(var i = 0; i < glTasks.length; i++) {
		var t = glTasks[i];
		var tr = document.createElement("tr");
		tr.appendChild(nodeTd(t.taskName));
		tr.appendChild(nodeTd(t.rebloggedDay+"/"+t.limitDay));
		tr.appendChild(nodeTd(t.reblogged+""));
		tr.appendChild(nodeTd(t.limited+""));
		var td = document.createElement("td");
		td.innerHTML = 
		`
		<span class="btn" onclick='showTask("${t.taskName}")'><span class="oi oi-document"></span></span>
		<span class="btn" onclick='moRefTask("${t.taskName}")'><span class="oi oi-reload"></span></span>
		<span class="btn" onclick='moDelTask("${t.taskName}")'><span class="oi oi-trash"></span></span>
		`;
		tr.appendChild(td);
	
		table.appendChild(tr);
	}
}
function initReblogger() {
	if(!isLoggedIn()) {
		window.location.replace("/");
		return;
	}
	glForm.taskName = document.getElementById("taskName");
	glForm.fromVote = document.getElementById("fromVote");
	glForm.fromVoteMin = document.getElementById("fromVoteMin");
	glForm.fromVoteMax = document.getElementById("fromVoteMax");
	glForm.fromAuthor = document.getElementById("fromAuthor");
	glForm.fromTag = document.getElementById("fromTag");
	glForm.withAllTitle = document.getElementById("withAllTitle");
	glForm.withOneTitle = document.getElementById("withOneTitle");
	glForm.withAllBody = document.getElementById("withAllBody");
	glForm.withOneBody = document.getElementById("withOneBody");
	glForm.withAllTag = document.getElementById("withAllTag");
	glForm.withOneTag = document.getElementById("withOneTag");
	glForm.withoutTitle = document.getElementById("withoutTitle");
	glForm.withoutBody = document.getElementById("withoutBody");
	glForm.withoutTag = document.getElementById("withoutTag");
	glForm.limitDay = document.getElementById("limitDay");
	glForm.okBtn = document.getElementById("okBtn");
	
	glForm.mainTasks = document.getElementById("mainTasks");
	glForm.mainTask = document.getElementById("mainTask");
	listTasks();
}
function showX(i) {
	if(i == 0) {
		glForm.mainTasks.hidden = false;
		glForm.mainTask.hidden = true;
	}
	else if(i == 1) {
		glForm.mainTasks.hidden = true;
		glForm.mainTask.hidden = false;
	}
}
function clearForm() {
	glForm.taskName.value = "";
	glForm.fromVote.value = "";
	glForm.fromVoteMin.value = "0";
	glForm.fromVoteMax.value = "100";
	glForm.fromAuthor.value = "";
	glForm.fromTag.value = "";
	glForm.withAllTitle.value = "";
	glForm.withOneTitle.value = "";
	glForm.withAllBody.value = "";
	glForm.withOneBody.value = "";
	glForm.withAllTag.value = "";
	glForm.withOneTag.value = "";
	glForm.withoutTitle.value = "";
	glForm.withoutBody.value = "";
	glForm.withoutTag.value = "";
	glForm.limitDay.value = "10";
}
function nodeTd(te) {
	var td = document.createElement("td");
	td.textContent = te;
	return td;
}
function getTask(taskName) {
	if(taskName == null || glTasks == null) return null;
	for(var i = 0; i < glTasks.length; i++) {
		var t = glTasks[i];
		if(t.taskName == taskName) return t;
	}
	return null;
}
function newTaskName(taskName) {
	if(taskName == null || taskName == "") taskName = "task";
	if(getTask(taskName) == null) return taskName;
	for(var i = 2; i < 10000; i++) {
		if(getTask(taskName+i) == null) return taskName+i;
	}
	return null;
}
function moRefTask(taskName) {
	showModal(`Refresh all limits/counters for task <b>${taskName}</b>?`, function() {
		var obj = { 
			"reblogAccount": username(),
			"refreshTask": taskName
		}
		postFormTask(obj)
	});
}
function moDelTask(taskName) {
	showModal(`Remove task <b>${taskName}</b>?`, function() {
		var obj = { 
			"reblogAccount": username(),
			"deleteTask": taskName
		}
		postFormTask(obj)
	});
}
function joinArray(arr) {
	return arr==null?"":(arr.join(" "));
} 
function showTask(taskName) {
	var t = getTask(taskName);
	if(t == null) {
		clearForm();
		taskName = newTaskName(taskName);
		glForm.taskName.value = taskName;
	}
	else {
		var a = joinArray;
		glForm.taskName.value = t.taskName;
		glForm.fromVote.value = a(t.fromVote);
		glForm.fromVoteMin.value = t.fromVoteMin+"";
		glForm.fromVoteMax.value = t.fromVoteMax+"";
		glForm.fromAuthor.value = a(t.fromAuthor);
		glForm.fromTag.value = a(t.fromTag);
		glForm.withAllTitle.value = a(t.withAllTitle);
		glForm.withOneTitle.value = a(t.withOneTitle);
		glForm.withAllBody.value = a(t.withAllBody);
		glForm.withOneBody.value = a(t.withOneBody);
		glForm.withAllTag.value = a(t.withAllTag);
		glForm.withOneTag.value = a(t.withOneTag);
		glForm.withoutTitle.value = a(t.withoutTitle);
		glForm.withoutBody.value = a(t.withoutBody);
		glForm.withoutTag.value = a(t.withoutTag);
		glForm.limitDay.value = t.limitDay+"";
	}
	showX(1);
}
function postFormTask(obj) {
	if(!isLoggedIn()) return;
	obj.token = HS_LOGIN_OBJECT.accessToken;
	sd("POST", "/form/task", obj, function(x) {
		var obj = x.response;
		if(obj.success == "logout") {
			hsLogOut();
			console.log("logout");
			return;
		}
		if(obj.success == "posting") {
			var hsURL = "https://hivesigner.com/authorize/hivereblogger";
			document.getElementById("addButton").hidden = true;
			document.getElementById("hivePostButton").hidden = false;
			console.log("posting");
			return;
		}
		var tasks = obj.tasks;
		glTasks = tasks;
		refreshTaskList();
		showX(0);
	}, function(x, e) {
		console.log(e);
	});
}
function postTask() {
	var obj = { 
		"reblogAccount": username(),
		"taskName": glForm.taskName.value,
		"fromVote": glForm.fromVote.value,
		"fromVoteMin": glForm.fromVoteMin.value,
		"fromVoteMax": glForm.fromVoteMax.value,
		"fromAuthor": glForm.fromAuthor.value,
		"fromTag": glForm.fromTag.value,
		"withAllTitle": glForm.withAllTitle.value,
		"withOneTitle": glForm.withOneTitle.value,
		"withAllBody": glForm.withAllBody.value,
		"withOneBody": glForm.withOneBody.value,
		"withAllTag": glForm.withAllTag.value,
		"withOneTag": glForm.withOneTag.value,
		"withoutTitle": glForm.withoutTitle.value,
		"withoutBody": glForm.withoutBody.value,
		"withoutTag": glForm.withoutTag.value,
		"limitDay": glForm.limitDay.value
	};
	if(obj.taskName == null || obj.taskName == "") return;
	postFormTask(obj);
}




