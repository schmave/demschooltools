package models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Model;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;
import com.feth.play.module.pa.user.NameIdentity;

@Entity
@Table(name = "users")
public class User extends Model {
	private static final long serialVersionUID = 1L;

    @ManyToOne()
    public Organization organization;

	@Id
	public Integer id;

	public String email;
	public String name;

	public boolean active;
	public boolean emailValidated;

    @OneToMany(mappedBy="user")
    public List<UserRole> roles;

	@OneToMany(cascade = CascadeType.ALL)
	public List<LinkedAccount> linkedAccounts;

	public static final Finder<Integer, User> find = new Finder<Integer, User>(
			User.class);

    public static User findById(Integer id) {
        return find.where().eq("organization", OrgConfig.get().org)
            .eq("id", id)
            .findUnique();
    }

    public static User create(String email, String name, Organization org) {
        User result = new User();

        result.email = email;
        result.name = name;
        result.organization = org;
        result.active = true;
        result.emailValidated = true;

        result.save();

        return result;
    }

    public boolean hasRole(String role) {
        for (UserRole r : roles) {
            if (r.role.equals(role) ||
                UserRole.includes(r.role, role)) {
                return true;
            }
        }

        return false;
    }

	public static boolean existsByAuthUserIdentity(
			final AuthUserIdentity identity) {
		final ExpressionList<User> exp = getAuthUserFind(identity);
		return exp.findRowCount() > 0;
	}

	public static User findByAuthUserIdentity(final AuthUserIdentity identity) {
		if (identity == null) {
			return null;
		}
		return getAuthUserFind(identity).findUnique();
	}

	public void merge(final User otherUser) {
		for (final LinkedAccount acc : otherUser.linkedAccounts) {
			this.linkedAccounts.add(LinkedAccount.create(acc));
		}
		// do all other merging stuff here - like resources, etc.

		// deactivate the merged user that got added to this one
		otherUser.active = false;
		Ebean.save(Arrays.asList(new User[] { otherUser, this }));
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
		final Set<String> providerKeys = new HashSet<String>(
				linkedAccounts.size());
		for (final LinkedAccount acc : linkedAccounts) {
			providerKeys.add(acc.providerKey);
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
		return getEmailUserFind(email).findUnique();
	}

    private static ExpressionList<User> getAuthUserFind(
            final AuthUserIdentity identity) {
        return find.where().eq("active", true)
                .eq("linkedAccounts.providerUserId", identity.getId())
                .eq("linkedAccounts.providerKey", identity.getProvider());
    }

	private static ExpressionList<User> getEmailUserFind(final String email) {
		return find.where()
                .eq("active", true)
                .eq("email", email);
	}

	public LinkedAccount getAccountByProvider(final String providerKey) {
		return LinkedAccount.findByProviderKey(this, providerKey);
	}

}
