/*
 *	$Id$
 *	This is an unpublished work copyright (c) 2018 HELIOS Software GmbH
 *	30827 Garbsen, Germany.
 */

package de.radioshuttle.net;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.radioshuttle.mqttpushclient.ActionsViewModel;
import de.radioshuttle.mqttpushclient.PushAccount;

public class ActionsRequest extends Request {
    public ActionsRequest(Context context, PushAccount pushAccount, MutableLiveData<Request> accountLiveData) {
        super(context, pushAccount, accountLiveData);
        mActions = new ArrayList<>();
    }

    public void addAction(ActionsViewModel.Action a) {
        mCmd = Cmd.CMD_ADD_ACTION;
        mActionArg = a;
    }

    public void deleteActions(List<String> actionNames) {
        mCmd = Cmd.CMD_REMOVE_ACTIONS;
        mActionListArg = actionNames;
    }

    public void updateAction(ActionsViewModel.Action a) {
        mCmd = Cmd.CMD_UPDATE_ACTION;
        mActionArg = a;
    }

    @Override
    public boolean perform() throws Exception {

        if (mCmd == Cmd.CMD_REMOVE_ACTIONS) {
            try {
                int[] rc = mConnection.deleteActions(mActionListArg);
                //TODO: handle rc
            } catch(MQTTException e) {
                requestErrorCode = e.errorCode;
                requestErrorTxt = e.getMessage();
            } catch(ServerError e) {
                requestErrorCode = e.errorCode;
                requestErrorTxt = e.getMessage();
            }
            requestStatus = mConnection.lastReturnCode;
        } else if (mCmd == Cmd.CMD_ADD_ACTION || mCmd == Cmd.CMD_UPDATE_ACTION) {
            try {
                int[] rc;
                Cmd.Action arg = new Cmd.Action();
                arg.topic = mActionArg.topic;
                arg.content = mActionArg.content;
                arg.retain = mActionArg.retain;

                if (mCmd == Cmd.CMD_ADD_ACTION) {
                    rc = mConnection.addAction(mActionArg.name, arg);
                } else {
                    rc = mConnection.updateAction(mActionArg.prevName, mActionArg.name, arg);
                }
                //TODO: handle rc
            } catch(MQTTException e) {
                requestErrorCode = e.errorCode;
                requestErrorTxt = e.getMessage();
            } catch(ServerError e) {
                requestErrorCode = e.errorCode;
                requestErrorTxt = e.getMessage();
            }
        }

        LinkedHashMap<String, Cmd.Action> result = mConnection.getActions();
        ArrayList<ActionsViewModel.Action> tmpRes = new ArrayList<>();
        if (mConnection.lastReturnCode == Cmd.RC_OK) {
            for(Iterator<Map.Entry<String, Cmd.Action>> it = result.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Cmd.Action> e = it.next();
                ActionsViewModel.Action a = new ActionsViewModel.Action();
                a.name = e.getKey();
                a.topic = e.getValue().topic;
                a.content = e.getValue().content;
                a.retain = e.getValue().retain;
                tmpRes.add(a);
            }
            Comparator c = new Comparator<ActionsViewModel.Action>() {
                @Override
                public int compare(ActionsViewModel.Action o1, ActionsViewModel.Action o2) {
                    String s1 = o1.name == null ? "" : o1.name;
                    String s2 = o2.name == null ? "" : o2.name;
                    return s1.compareTo(s2);
                }
            };
            Collections.sort(tmpRes, c);
            mActions = tmpRes;
        } else {
            //TODO: rare case: removeActions ok, but getActions() failed. see TopicsRequest
        }

        return true;
    }

    public int requestStatus;
    public int requestErrorCode;
    public String requestErrorTxt;

    public int mCmd;
    public List<String> mActionListArg;
    public ActionsViewModel.Action mActionArg;

    public ArrayList<ActionsViewModel.Action> mActions;

}