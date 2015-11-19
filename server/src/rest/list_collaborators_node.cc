/*
 * list_collaborators_node.cc
 *
 *  Created on: 4/10/2015
 *      Author: Martin
 */

#include "list_collaborators_node.h"

using std::string;
using std::stringstream;
using std::vector;

ListCollaboratorsNode::ListCollaboratorsNode(MgConnectionW&  conn)  : Node(conn) {
}

ListCollaboratorsNode::~ListCollaboratorsNode() {
}

vector<string> ListCollaboratorsNode::split(const string &s, char delim) {
    stringstream ss(s);
    string item;
    vector<string> tokens;
    while (getline(ss, item, delim)) {
        tokens.push_back(item);
    }
    return tokens;
}

void ListCollaboratorsNode::executeGet() {
	vector<string> lista=ListCollaboratorsNode::split(getUri(),'/');
	string fileId ="";
	int status=11;
	Log(Log::LogMsgDebug) << "[ListCollaboratorsNode] ";

	if ( (lista[4]=="files") and (lista[6]=="collaborators") ) {

		string userId=getUserId();
		fileId =lista[5];
		bool result=false;
		Log(Log::LogMsgDebug) << "[ListCollaboratorsNode], UserId: " <<userId<< ", fileId: " << fileId;
		std::ostringstream item;

		vector<RequestDispatcher::user_info_st> lista_user_info;
		item << "[";

		RequestDispatcher::file_info_st file_info;

		if (getRequestDispatcher()->get_file_info(userId,fileId,file_info,status)) {
			lista_user_info=file_info.shared_users;
			Log(Log::LogMsgDebug) << "[ListOwnersNode]: list owners users " << lista_user_info.size();
			if (lista_user_info.size() != 0) {
				for (int i = 0; i < lista_user_info.size() - 1; ++i) {
					item
					<< "{\"id\":\"" << lista_user_info[i].id << "\","
					<< "\"firstName\":\"" << lista_user_info[i].first_name << "\","
					<< "\"lastName\":\"" << lista_user_info[i].last_name << "\","
					<< "\"email\":\"" << lista_user_info[i].email << "\"},";
					result = true;
				}
				item
				<< "{\"id\":\"" << lista_user_info[lista_user_info.size() - 1].id << "\","
				<< "\"firstName\":\"" << lista_user_info[lista_user_info.size() - 1].first_name << "\","
				<< "\"lastName\":\"" << lista_user_info[lista_user_info.size() - 1].last_name << "\","
				<< "\"email\":\"" << lista_user_info[lista_user_info.size() - 1].email << "\"}";
				result = true;
				Log(Log::LogMsgDebug) << "[ListOwnersNode] last node added ";
			} else {
				result = true;
			}
		}

		item << "]";

		if (!result) {
			getConnection().sendStatus(MgConnectionW::STATUS_CODE_NO_CONTENT);
			getConnection().sendContentType(MgConnectionW::CONTENT_TYPE_JSON);
			string msg = handlerError(status);
			getConnection().printfData(msg.c_str());
		} else {
			getConnection().sendStatus(MgConnectionW::STATUS_CODE_OK);
			getConnection().sendContentType(MgConnectionW::CONTENT_TYPE_JSON);

			const std::string tmp = item.str();
			const char* msg = tmp.c_str();
			Log(Log::LogMsgDebug) << msg;
			getConnection().printfData(msg);
		}
	}
	else{
		getConnection().sendStatus(MgConnectionW::STATUS_CODE_BAD_REQUEST);
		getConnection().sendContentType(MgConnectionW::CONTENT_TYPE_JSON);
		string msg=handlerError(status);
		getConnection().printfData(msg.c_str());
	}
}
std::string ListCollaboratorsNode::defaultResponse(){
	return "[]";
}

std::string ListCollaboratorsNode::getUserId(){
	vector<string> lista=ListCollaboratorsNode::split(getUri(),'/');
	return lista[3];
}

