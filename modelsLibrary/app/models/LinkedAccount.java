package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import io.ebean.*;

import com.feth.play.module.pa.user.AuthUser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class LinkedAccount extends Model {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Id
    private Long id;

	@ManyToOne
    private User user;

    private String providerUserId;
    private String providerKey;

	public static final Finder<Long, LinkedAccount> find = new Finder<>(
			LinkedAccount.class);

	public static LinkedAccount create(final AuthUser authUser) {
		final LinkedAccount ret = new LinkedAccount();
		ret.update(authUser);
		return ret;
	}

	public void update(final AuthUser authUser) {
		this.providerKey = authUser.getProvider();
		this.providerUserId = authUser.getId();
	}

	public static LinkedAccount create(final LinkedAccount acc) {
		final LinkedAccount ret = new LinkedAccount();
		ret.providerKey = acc.providerKey;
		ret.providerUserId = acc.providerUserId;

		return ret;
	}
}