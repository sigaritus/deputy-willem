/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package info.dourok.dict;

/**
 *
 * @author drcher
 */
public interface DictCommunication {

    final static int MSG_QUERY = 1;
    final static int MSG_QUERY_FINISHED = 2;
    final static int MSG_BIND_CLIPBOARD = 3;
    final static int MSG_UNBIND_CLIPBOARD = 4;
    final static int MSG_BIND_NOTIFICATION = 5;
    final static int MSG_UNBIND_NOTIFICATION = 6;
    final static int MSG_KILL_YOURSELF = 7;  //必须是最后一项
}
