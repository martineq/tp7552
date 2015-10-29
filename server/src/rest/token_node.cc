#include "token_node.h"
#include "../lib/json/json.h"

#include <ctime>
#include <sstream>
#include <cstring>
#include <stdlib.h>
#include <stdio.h>
#include <vector>
#include "../util/random_number.h"
#include "../util/md5.h"

using std::vector;
using std::string;
using std::stringstream;

struct email_pass{
	string email="";
	string pass="";
};

typedef struct email_pass email_pass;

TokenNode::TokenNode() : Node("token") {
}

void TokenNode::executePost(MgConnectionW& conn, const char* url){
	const char *s = conn->content;
	char body[1024*sizeof(char)] = "";
	strncpy(body, s, conn->content_len);
	body[conn->content_len] = '0';
	// Parse the JSON body
	Json::Value root;
	Json::Reader reader;
	bool parsedSuccess = reader.parse(body, root, false);
	if (!parsedSuccess) {
		// Error, do something
	}
	const Json::Value mail = root["email"];
	const Json::Value pass = root["password"];
	std::string email = mail.asString();
	std::string password = pass.asString();

	Log(Log::LogMsgDebug) << "[" << "Validando usuario" << "] " << email << " " << password;

	string new_token=CreateToken(email);
	string userId="";
	int status;

	if (!this->rd->log_in(email, password, new_token, userId, status)){
		Log(Log::LogMsgDebug) << "[" << "email incorrecto" << "] ";
		conn.sendStatus(MgConnectionW::STATUS_CODE_NO_CONTENT);
		conn.sendContentType(MgConnectionW::CONTENT_TYPE_JSON);
		conn.printfData("{ \"userId\": \"%d\",  \"email\": \"%s\",  \"token\": \"%s\" }", 0, "", "");
	}else{

		Log(Log::LogMsgDebug) << "[" << "Valid user: " << userId << "] ";
		conn.sendStatus(MgConnectionW::STATUS_CODE_OK);
		conn.sendContentType(MgConnectionW::CONTENT_TYPE_JSON);
		conn.printfData("{ \"userId\": \"%s\",  \"email\": \"%s\",  \"token\": \"%s\" }", userId.c_str(), email.c_str(), new_token.c_str());
	}
}


string TokenNode::CreateToken(const std::string& email){
	stringstream ss;
	ss << randomNumber(1000) << email << time(NULL) << randomNumber(9999) ;
	string out;
	md5Str(out, ss.str());
	return out;
}

void TokenNode::setRequestDispatcher(RequestDispatcher* rd){
	this->rd=rd;
}


