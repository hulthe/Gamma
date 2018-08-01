import React from "react";
import {
  GammaCardDisplayTitle,
  GammaCardBody,
  GammaCard
} from "../../../../common-ui/design";

import GammaTextField from "../../../../common/elements/gamma-text-field";
import IfElseRendering from "../../../../common/declaratives/if-else-rendering";
import { Center } from "../../../../common-ui/layout";
import UserForm from "../common-views/user-form/UserForm.view";
import GammaTranslations from "../../../../common/declaratives/gamma-translations";
import translations from "./EditUserDetails.screen.translations.json";

const EditUserDetails = ({ user }) => (
  <IfElseRendering
    test={user != null}
    ifRender={() => (
      <GammaTranslations
        translations={translations}
        uniquePath="Users.Screen.EditUserDetails"
        render={text => (
          <Center>
            <UserForm
              titleText={text.EditGroups}
              submitText={text.SaveGroups}
              initialValues={{
                ...user,
                acceptanceYear: user.acceptanceYear + ""
              }}
              onSubmit={(values, actions) => {
                console.log(values);
              }}
            />
          </Center>
        )}
      />
    )}
  />
);

export default EditUserDetails;
