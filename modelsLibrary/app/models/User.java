package models;

import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import io.ebean.*;
import io.ebean.ExpressionList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends Model {
	public final static String DUMMY_USERNAME = "__DUMMY_USERNAME__";
	private static final long serialVersionUID = 1L;

    @ManyToOne()
    private Organization organization;

	@Id
    private Integer id;

    private String email;
    private String name;
    private String hashedPassword = "";

    private boolean active;

    private boolean emailValidated;

    @OneToMany(mappedBy="user")
    public List<UserRole> roles;

	@OneToMany(cascade = CascadeType.ALL)
	public List<LinkedAccount> linkedAccounts;

	public static final Finder<Integer, User> find = new Finder<>(
			User.class);

    public static User findById(Integer id, Organization org) {
        return find.query().where().eq("organization", org)
            .eq("id", id)
            .findOne();
    }

    public static User create(String email, String name, Organization org) {
        User result = new User();

        result.email = email.toLowerCase();
        result.name = name;
        result.organization = org;
        result.active = true;
        result.emailValidated = true;

        result.save();

        return result;
    }

    public boolean hasRole(String role) {
    	if (this.name.equals(DUMMY_USERNAME)) {
    		return false;
    	}
        for (UserRole r : roles) {
            if (r.getRole().equals(role) ||
                UserRole.includes(r.getRole(), role)) {
                return true;
            }
        }

        return false;
    }

	public static boolean existsByAuthUserIdentity(
			final AuthUserIdentity identity) {
		final ExpressionList<User> exp = getAuthUserFind(identity);
		return exp.findCount() > 0;
	}

	public static User findByAuthUserIdentity(final AuthUserIdentity identity) {
		if (identity == null) {
			return null;
		}
		if (identity.getProvider().equals("evan-auth-provider")) {
			return User.findByEmail(identity.getId());
		}
		return getAuthUserFind(identity).findOne();
	}

	public void merge(final User otherUser) {
		for (final LinkedAccount acc : otherUser.linkedAccounts) {
			this.linkedAccounts.add(LinkedAccount.create(acc));
		}
		// do all other merging stuff here - like resources, etc.

		// deactivate the merged user that got added to this one
		otherUser.active = false;
		DB.save(Arrays.asList(otherUser, this));
	}

	//public static User create(final AuthUser authUser) {
	//	final User user = new User();
	//	user.active = true;
	//	user.linkedAccounts = Collections.singletonList(LinkedAccount
	//			.create(authUser));
    //
	//	if (authUser instanceof EmailIdentity) {
	//		final EmailIdentity identity = (EmailIdentity) authUser;
	//		// Remember, even when getting them from FB & Co., emails should be
	//		// verified within the application as a security breach there might
	//		// break your security as well!
	//		user.email = identity.getEmail();
	//		user.emailValidated = false;
	//	}
    //
	//	if (authUser instanceof NameIdentity) {
	//		final NameIdentity identity = (NameIdentity) authUser;
	//		final String name = identity.getName();
	//		if (name != null) {
	//			user.name = name;
	//		}
	//	}
    //
	//	user.save();
	//	return user;
	//}

	public static void merge(final AuthUser oldUser, final AuthUser newUser) {
		User.findByAuthUserIdentity(oldUser).merge(
				User.findByAuthUserIdentity(newUser));
	}

	public Set<String> getProviders() {
		final Set<String> providerKeys = new HashSet<>(
				linkedAccounts.size());
		for (final LinkedAccount acc : linkedAccounts) {
			providerKeys.add(acc.getProviderKey());
		}
		return providerKeys;
	}

	public static void addLinkedAccount(final AuthUser oldUser,
			final AuthUser newUser) {
		final User u = User.findByAuthUserIdentity(oldUser);
		u.linkedAccounts.add(LinkedAccount.create(newUser));
		u.save();
	}

	public static User findByEmail(final String email) {
		return getEmailUserFind(email).findOne();
	}

    private static ExpressionList<User> getAuthUserFind(
            final AuthUserIdentity identity) {
        return find.query().where()
                .eq("linkedAccounts.providerUserId", identity.getId())
                .eq("linkedAccounts.providerKey", identity.getProvider());
    }

	private static ExpressionList<User> getEmailUserFind(final String email) {
		return find.query().where().ieq("email", email);
	}
}