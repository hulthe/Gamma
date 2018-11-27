import React from "react";

import { Switch, Route } from "react-router-dom";

import ShowAllGroups from "./screens/show-all-groups";
import CreateNewGroup from "./screens/create-new-group";
import EditGroupDetails from "./screens/edit-group-details";
import ShowGroupDetails from "./screens/show-group-details";
import AddUsersToGroup from "./screens/add-users-to-group";

class Groups extends React.Component {
    constructor(props) {
        super();

        props.groupsLoad().then(response => {
            props.gammaLoadingFinished();
        });
    }

    render() {
        return (
            <Switch>
                <Route path="/groups" exact component={ShowAllGroups} />
                <Route path="/groups/new" exact component={CreateNewGroup} />
                <Route
                    path="/groups/:id/edit"
                    exact
                    component={EditGroupDetails}
                />
                <Route
                    path="/groups/:id/members"
                    exact
                    component={AddUsersToGroup}
                />
                <Route path="/groups/:id" exact component={ShowGroupDetails} />
            </Switch>
        );
    }
}

export default Groups;
