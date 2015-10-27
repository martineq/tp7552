#ifndef REQUESTDISPATCHER_H
#define REQUESTDISPATCHER_H

#include <iostream>
#include <string>
#include <vector>
#include <sstream>

#include "data_handler.h"
#include "file_handler.h"

class RequestDispatcher{

  private:
    DataHandler dh_;
    FileHandler fh_;
    size_t max_user_quota_;
    
    bool check_token(string user_id, string user_token, int& status);
    bool get_user_quota_used(string user_id, string& quota, int& status);
    bool increase_user_quota_used(string user_id, string quota_increased, int& status);
    bool decrease_user_quota_used(string user_id, string quota_decreased, int& status); 
    bool change_dir_date(string dir_id, string new_date, int& status);
    bool get_root_dir_id(string user_id, string& root_dir_id, int& status);
    
  public:
    RequestDispatcher();
    ~RequestDispatcher();
    vector<string> split_string(string string_to_split, char delimiter);

    bool init(string database_path,size_t max_user_quota);
        
    bool sign_up(string email, string password, string name, string location, string new_token, string date, string& user_id, int& status);
    bool log_in(string email, string password, string new_token, string& user_id, int& status);
    
    bool new_directory(string user_id, string user_token, string name, string date, string parent_dir_id, string& dir_id, int& status);
    bool new_file(string user_id, string user_token, string name, string extension, string date, const char* p_file_stream, string size, string parent_dir_id, string& file_id, int& status);

    bool get_user_info(string user_id, string user_token, DataHandler::user_info_st& user_info, int& status);
    bool get_directory_info(string user_id, string user_token, string dir_id, DataHandler::dir_info_st& dir_info, int& status);
    bool get_file_info(string user_id, string user_token, string file_id, DataHandler::file_info_st& file_info, int& status);
    bool get_file_stream(string user_id, string user_token, string file_id, string revision, char*& p_file_stream, size_t& size_stream, int& status);

    bool delete_user(string user_id, string user_token, int& status);
    bool delete_directory(string user_id, string user_token, string dir_id, int& status);
    bool delete_file(string user_id, string user_token, string file_id, int& status);

//     bool modify_user_info(string user_id, string email, string password, string name, string location, string files_shared, int& status);
//     bool modify_directory_info(string dir_id, string name, string date, string tags, int& status);
//     bool modify_file_info(string file_id, string name, string extension, string date, string tags, string users_shared, string user_id_modifier, int& status);

};

#endif // REQUESTDISPATCHER_H

/*
Clase Request Dispatcher

TODO (mart): ¿Cómo se van a manejar las revisiones? Ver si hay que agregar parent_revision en cada file.

Casos de uso y funciones de Data Handler relacionadas: 
+ Modificar info usr        -> get_user_token(), modify_user_info().            TODO: Verificar que el usr sea dueño.
+ Modificar info dir        -> get_user_token(), modify_directory_info().       TODO: Verificar que el usr sea dueño.
+ Modificar info arch       -> get_user_token(), modify_file_info().            TODO: En caso de ser dueño, verificar si el archivo está compartido y quitar ese estado a los demás usuarios. En caso de ser compartido sólo quitarse de la lista de compartidos.
*/