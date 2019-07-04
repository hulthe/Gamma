package it.chalmers.gamma.controller;

import static it.chalmers.gamma.db.serializers.ITUserSerializer.Properties.ACCEPTANCE_YEAR;
import static it.chalmers.gamma.db.serializers.ITUserSerializer.Properties.CID;
import static it.chalmers.gamma.db.serializers.ITUserSerializer.Properties.FIRST_NAME;
import static it.chalmers.gamma.db.serializers.ITUserSerializer.Properties.ID;
import static it.chalmers.gamma.db.serializers.ITUserSerializer.Properties.LAST_NAME;
import static it.chalmers.gamma.db.serializers.ITUserSerializer.Properties.NICK;

import it.chalmers.gamma.db.entity.ITUser;
import it.chalmers.gamma.db.entity.WebsiteInterface;
import it.chalmers.gamma.db.entity.WebsiteURL;
import it.chalmers.gamma.db.entity.Whitelist;
import it.chalmers.gamma.db.serializers.ITUserSerializer;
import it.chalmers.gamma.requests.CreateGroupRequest;
import it.chalmers.gamma.requests.CreateITUserRequest;
import it.chalmers.gamma.requests.EditITUserRequest;
import it.chalmers.gamma.requests.ResetPasswordFinishRequest;
import it.chalmers.gamma.requests.ResetPasswordRequest;
import it.chalmers.gamma.response.CodeExpiredResponse;
import it.chalmers.gamma.response.CodeOrCidIsWrongResponse;
import it.chalmers.gamma.response.EditedProfilePicture;
import it.chalmers.gamma.response.FileNotSavedException;
import it.chalmers.gamma.response.InputValidationFailedResponse;
import it.chalmers.gamma.response.PasswordChangedResponse;
import it.chalmers.gamma.response.PasswordResetResponse;
import it.chalmers.gamma.response.PasswordTooShortResponse;
import it.chalmers.gamma.response.UserAlreadyExistsResponse;
import it.chalmers.gamma.response.UserCreatedResponse;
import it.chalmers.gamma.response.UserEditedResponse;
import it.chalmers.gamma.response.UserNotFoundResponse;
import it.chalmers.gamma.service.ActivationCodeService;
import it.chalmers.gamma.service.ITUserService;
import it.chalmers.gamma.service.MailSenderService;
import it.chalmers.gamma.service.MembershipService;
import it.chalmers.gamma.service.PasswordResetService;
import it.chalmers.gamma.service.UserWebsiteService;
import it.chalmers.gamma.service.WhitelistService;
import it.chalmers.gamma.util.ImageITUtils;
import it.chalmers.gamma.util.InputValidationUtils;
import it.chalmers.gamma.util.TokenUtils;
import it.chalmers.gamma.views.WebsiteView;

import java.io.IOException;
import java.security.Principal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.json.simple.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("PMD.ExcessiveImports")
@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public final class ITUserController {

    private final ITUserService itUserService;
    private final ActivationCodeService activationCodeService;
    private final WhitelistService whitelistService;
    private final UserWebsiteService userWebsiteService;
    private final MembershipService membershipService;
    private final PasswordResetService passwordResetService;
    private final MailSenderService mailSenderService;

    public ITUserController(ITUserService itUserService,
                            ActivationCodeService activationCodeService,
                            WhitelistService whitelistService,
                            UserWebsiteService userWebsiteService,
                            MembershipService membershipService,
                            PasswordResetService passwordResetService,
                            MailSenderService mailSenderService) {
        this.itUserService = itUserService;
        this.activationCodeService = activationCodeService;
        this.whitelistService = whitelistService;
        this.userWebsiteService = userWebsiteService;
        this.membershipService = membershipService;
        this.passwordResetService = passwordResetService;
        this.mailSenderService = mailSenderService;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public ResponseEntity<String> createUser(
            @Valid @RequestBody CreateITUserRequest createITUserRequest, BindingResult result) {
        if (result.hasErrors()) {
            throw new InputValidationFailedResponse(InputValidationUtils.getErrorMessages(result.getAllErrors()));
        }
        Whitelist user = this.whitelistService.getWhitelist(
                createITUserRequest.getWhitelist().getCid()
        );

        if (user == null) {
            throw new UserNotFoundResponse();
        }

        createITUserRequest.setWhitelist(user);

        if (this.itUserService.userExists(createITUserRequest.getWhitelist().getCid())) {
            throw new UserAlreadyExistsResponse();
        }

        if (!this.activationCodeService.codeMatches(createITUserRequest.getCode(), user.getCid())) {
            throw new CodeOrCidIsWrongResponse();
        }

        if (this.activationCodeService.hasCodeExpired(user.getCid(), 2)) {
            this.activationCodeService.deleteCode(user.getCid());
            throw new CodeExpiredResponse();
        }

        int minPassLength = 8;

        if (createITUserRequest.getPassword().length() < minPassLength) {
            throw new PasswordTooShortResponse();
        } else {
            this.itUserService.createUser(
                    createITUserRequest.getNick(),
                    createITUserRequest.getFirstName(),
                    createITUserRequest.getLastName(),
                    createITUserRequest.getWhitelist().getCid(),
                    Year.of(createITUserRequest.getAcceptanceYear()),
                    createITUserRequest.isUserAgreement(),
                    null,
                    createITUserRequest.getPassword());
            removeCidFromWhitelist(createITUserRequest);
            return new UserCreatedResponse();
        }
    }


    // Check if this cascades automatically
    private void removeCidFromWhitelist(CreateITUserRequest createITUserRequest) {
        this.activationCodeService.deleteCode(createITUserRequest.getWhitelist().getCid());
        this.whitelistService.removeWhiteListedCID(createITUserRequest.getWhitelist().getCid());
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public JSONObject getMe(Principal principal) {
        String cid = principal.getName();
        ITUser user = this.itUserService.loadUser(cid);
        ITUserSerializer serializer =
                new ITUserSerializer(ITUserSerializer.Properties.getAllProperties());
        List<WebsiteView> websites =
                this.userWebsiteService.getWebsitesOrdered(
                        this.userWebsiteService.getWebsites(user)
                );
        return serializer.serialize(user, websites,
                ITUserSerializer.getGroupsAsJson(this.membershipService.getMembershipsByUser(user)));
    }

    @RequestMapping(value = "/minified", method = RequestMethod.GET)
    public List<JSONObject> getAllUserMini() {
        List<ITUser> itUsers = this.itUserService.loadAllUsers();
        List<ITUserSerializer.Properties> props =
                new ArrayList<>(Arrays.asList(
                        CID,
                        FIRST_NAME,
                        LAST_NAME,
                        NICK,
                        ACCEPTANCE_YEAR,
                        ID
                ));
        List<JSONObject> minifiedITUsers = new ArrayList<>();
        ITUserSerializer serializer = new ITUserSerializer(props);
        for (ITUser user : itUsers) {
            minifiedITUsers.add(serializer.serialize(user, null, null));
        }
        return minifiedITUsers;
    }
    /**
    * First tries to get user using id, if not found gets it using the cid.
    */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public JSONObject getUser(@PathVariable("id") String id) {
        ITUser user;
        try {
            user = this.itUserService.getUserById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            user = this.itUserService.loadUser(id);
            if (user == null) {
                throw new UserNotFoundResponse();
            }
        }

        ITUserSerializer serializer = new ITUserSerializer(
                ITUserSerializer.Properties.getAllProperties()
        );
        List<WebsiteView> websites =
                this.userWebsiteService.getWebsitesOrdered(
                        this.userWebsiteService.getWebsites(user)
                );

        return serializer.serialize(user, websites,
                ITUserSerializer.getGroupsAsJson(this.membershipService.getMembershipsByUser(user)));
    }

    @RequestMapping(value = "/me", method = RequestMethod.PUT)
    public ResponseEntity<String> editMe(Principal principal, @RequestBody EditITUserRequest request) {
        String cid = principal.getName();
        ITUser user = this.itUserService.loadUser(cid);
        if (user == null) {
            throw new UserNotFoundResponse();
        }
        this.itUserService.editUser(user.getId(), request.getNick(), request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPhone(), request.getLanguage());
        List<CreateGroupRequest.WebsiteInfo> websiteInfos = request.getWebsites();
        List<WebsiteURL> websiteURLs = new ArrayList<>();
        List<WebsiteInterface> userWebsite = new ArrayList<>(
                this.userWebsiteService.getWebsites(user)
        );
        this.userWebsiteService.addWebsiteToEntity(websiteInfos, userWebsite);
        this.userWebsiteService.addWebsiteToUser(user, websiteURLs);
        return new UserEditedResponse();
    }

    @RequestMapping(value = "/me/avatar", method = RequestMethod.PUT)
    public ResponseEntity<String> editProfileImage(Principal principal, @RequestParam MultipartFile file) {
        String cid = principal.getName();
        ITUser user = this.itUserService.loadUser(cid);
        if (user == null) {
            throw new UserNotFoundResponse();
        }
        try {
            String fileUrl = ImageITUtils.saveImage(file);
            this.itUserService.editProfilePicture(user, fileUrl);
        } catch (IOException e) {
            throw new FileNotSavedException();
        }

        return new EditedProfilePicture();
    }

    @RequestMapping(value = "/reset_password", method = RequestMethod.POST)
    public ResponseEntity<String> resetPasswordRequest(
            @Valid @RequestBody ResetPasswordRequest request, BindingResult result) {
        System.out.println(request);
        if (result.hasErrors()) {
            throw new InputValidationFailedResponse(InputValidationUtils.getErrorMessages(result.getAllErrors()));
        }
        if (!this.itUserService.userExists(request.getCid())) {
            throw new UserNotFoundResponse();
        }
        ITUser user = this.itUserService.loadUser(request.getCid());
        String token = TokenUtils.generateToken();
        if (this.passwordResetService.userHasActiveReset(user)) {
            this.passwordResetService.editToken(user, token);
        } else {
            this.passwordResetService.addToken(user, token);
        }
        sendMail(user, token);
        return new PasswordResetResponse();
    }

    @RequestMapping(value = "/reset_password/finish", method = RequestMethod.PUT)
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordFinishRequest request, BindingResult result) {
        if (result.hasErrors()) {
            throw new InputValidationFailedResponse(InputValidationUtils.getErrorMessages(result.getAllErrors()));
        }
        if (!this.itUserService.userExists(request.getCid())) {
            throw new UserNotFoundResponse();
        }
        ITUser user = this.itUserService.loadUser(request.getCid());
        if (!this.passwordResetService.userHasActiveReset(user)
                || !this.passwordResetService.tokenMatchesUser(user, request.getToken())) {
            throw new CodeOrCidIsWrongResponse();
        }
        this.itUserService.setPassword(user, request.getPassword());
        this.passwordResetService.removeToken(user);
        return new PasswordChangedResponse();
    }

    // TODO Make sure that an URL is added to the email
    private void sendMail(ITUser user, String token) {
        String subject = "Password reset for Account at IT division of Chalmers";
        String message = "A password reset have been requested for this account, if you have not requested "
                + "this mail, feel free to ignore it. \n Your reset code : " + token + "URL : ";
        this.mailSenderService.trySendingMail(user.getCid(), subject, message);
    }
}
