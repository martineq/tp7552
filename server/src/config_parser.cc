#include "config_parser.h"

ConfigParser::ConfigParser(){

}

ConfigParser::~ConfigParser(){

}


/**
 * @brief Reads the content of the config file for server. Returns true on success.
 * 
 * @param connection return connection information from file
 * @return bool
 */
bool ConfigParser::load_configuration(ConfigParser::stConnection& connection){

  // Open file
  std::ifstream config_file(YAML_CONFIG_FILE);
  bool status_ok = true;

  // Parsing file
  if(config_file.good()==true){
    try{
      // Starts to read YAML file
      YAML::Parser parser(config_file);
      
      // Creates and obtains the root node of YAML file
      YAML::Node root_node;
      parser.GetNextDocument(root_node);
      
      // Reads the content
      load_content(root_node,connection);
          
    }catch(YAML::Exception& e){
      notify_read_error(__FILE__,__LINE__,e.what(),status_ok);
    }

  }else{
    notify_read_error(__FILE__,__LINE__,"Error opening config file. ",status_ok);
  }

  return status_ok;
}


void ConfigParser::load_content(YAML::Node& root_node, ConfigParser::stConnection& connection){

  // Iterate nodes, reading information
  for(YAML::Iterator it=root_node.begin() ; it!=root_node.end() ; ++it){
    std::string key;
    key = read_yaml_node_to_string(it.first());
    
    if( key.compare(YAML_LABEL_IP) == 0 ){ connection.ip = read_yaml_node_to_string(it.second());
    }else if ( key.compare(YAML_LABEL_PORT) == 0 ){ connection.port = read_yaml_node_to_string(it.second());
    }else{ /* Future keys */ }

  }

  return void();
}


std::string ConfigParser::read_yaml_node_to_string(const YAML::Node& node){
  
  std::string value;
  bool read_ok = true;
  
  // Reads the value
  try{
    node >> value;
  }catch(YAML::Exception& e){
    notify_read_error(__FILE__,__LINE__,e.what(),read_ok);
  }

  if(read_ok == false){ value.assign(YAML_EMPTY_STRING); }
  
  return value;
}


void ConfigParser::notify_read_error(std::string file, int line, std::string msg, bool& read_ok){
  std::string message_error;
  message_error.append("YAML sintax error. Report: ");
  message_error.append(msg);
  std::cerr << message_error << std::endl;
  std::cerr <<"file: " << file << " - line: "<< line << message_error << std::endl;
  read_ok = false;
}