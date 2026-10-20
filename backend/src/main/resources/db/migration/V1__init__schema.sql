create table content_polls
(
    id                     bigint auto_increment
        primary key,
    created_at             datetime(6)             not null,
    updated_at             datetime(6)             not null,
    confirmed_candidate_id bigint                  null,
    deadline               datetime(6)             null,
    meeting_id             bigint                  not null,
    status                 enum ('CLOSED', 'OPEN') not null,
    constraint uk_content_polls_meeting
        unique (meeting_id)
);

create table content_candidates
(
    id                   bigint auto_increment
        primary key,
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    created_by_member_id bigint       not null,
    description          varchar(500) null,
    title                varchar(100) not null,
    content_poll_id      bigint       not null,
    constraint FKeeex2dkoqwaddv9qkedf0uhcb
        foreign key (content_poll_id) references content_polls (id)
);

create table content_votes
(
    id                   bigint auto_increment
        primary key,
    created_at           datetime(6)                             not null,
    updated_at           datetime(6)                             not null,
    meeting_member_id    bigint                                  not null,
    preference           enum ('AVAILABLE', 'DISLIKE', 'PREFER') not null,
    content_candidate_id bigint                                  not null,
    constraint uk_content_votes_candidate_member
        unique (content_candidate_id, meeting_member_id),
    constraint FKshma5g59v0v6cmexhij5b2ffd
        foreign key (content_candidate_id) references content_candidates (id)
);

create table expense_registrations
(
    id          bigint auto_increment
        primary key,
    expense_id  bigint      not null,
    fingerprint varchar(64) not null,
    meeting_id  bigint      not null,
    member_id   bigint      not null,
    request_key varchar(36) not null,
    constraint uk_expense_registration
        unique (meeting_id, member_id, request_key)
);

create table expenses
(
    id                  bigint auto_increment
        primary key,
    amount              bigint        not null,
    created_at          datetime(6)   not null,
    meeting_id          bigint        not null,
    memo                varchar(2000) null,
    payer_member_id     bigint        not null,
    receipt_key         varchar(500)  null,
    remainder_member_id bigint        null,
    rounding_unit       int           null,
    split_mode          varchar(10)   null,
    title               varchar(255)  not null
);

create table expense_participants
(
    expense_id bigint not null,
    member_id  bigint not null,
    primary key (expense_id, member_id),
    constraint FKc65ugmdfhjd6lxjx871fjkjxp
        foreign key (expense_id) references expenses (id)
);

create table expense_shares
(
    expense_id   bigint not null,
    share_amount bigint not null,
    member_id    bigint not null,
    primary key (expense_id, member_id),
    constraint FKdn6vq7keotg18eshxo5beagf2
        foreign key (expense_id) references expenses (id)
);

create table notifications
(
    id           bigint auto_increment
        primary key,
    created_at   datetime(6)                                                     not null,
    content      varchar(500)                                                    null,
    is_read      bit                                                             not null,
    meeting_id   bigint                                                          null,
    receiver_id  bigint                                                          not null,
    redirect_url varchar(255)                                                    null,
    title        varchar(100)                                                    not null,
    type         enum ('CONTENT_CLOSED', 'SCHEDULE_CLOSED', 'SETTLEMENT_CLOSED') not null
);

create table settlement_accounts
(
    id             bigint auto_increment
        primary key,
    account_holder varchar(100) not null,
    account_number varchar(50)  not null,
    bank_name      varchar(100) not null,
    meeting_id     bigint       not null,
    member_id      bigint       not null,
    constraint UK4euahcs3nh8heryavao4p5aa0
        unique (meeting_id, member_id)
);

create table settlements
(
    id                  bigint auto_increment
        primary key,
    closed_at           datetime(6)             null,
    closed_by_member_id bigint                  null,
    meeting_id          bigint                  not null,
    status              enum ('CLOSED', 'OPEN') not null,
    version             bigint                  not null,
    constraint UKdxf2932btiltv9chr2xsmemvr
        unique (meeting_id)
);

create table settlement_account_snapshots
(
    settlement_id  bigint       not null,
    account_holder varchar(100) null,
    account_number varchar(50)  null,
    bank_name      varchar(100) null,
    member_id      bigint       null,
    account_order  int          not null,
    primary key (settlement_id, account_order),
    constraint FK5a6unxmbj38cnl1aqoi0vdo5d
        foreign key (settlement_id) references settlements (id),
    check (`account_order` >= 0)
);

create table settlement_balances
(
    settlement_id bigint not null,
    member_id     bigint null,
    paid_amount   bigint null,
    share_amount  bigint null,
    balance_order int    not null,
    primary key (settlement_id, balance_order),
    constraint FKbjchrn1eydhcj4lp86lvaeva8
        foreign key (settlement_id) references settlements (id),
    check (`balance_order` >= 0)
);

create table settlement_transfers
(
    settlement_id  bigint not null,
    amount         bigint null,
    recipient_id   bigint null,
    sender_id      bigint null,
    transfer_order int    not null,
    primary key (settlement_id, transfer_order),
    constraint FKje88q29j4fcffdcpgx30varw5
        foreign key (settlement_id) references settlements (id),
    check (`transfer_order` >= 0)
);

create table users
(
    id            bigint auto_increment
        primary key,
    created_at    datetime(6)                  not null,
    updated_at    datetime(6)                  not null,
    deleted_at    datetime(6)                  null,
    email         varchar(255)                 not null,
    nickname      varchar(255)                 not null,
    password      varchar(255)                 not null,
    refresh_token varchar(255)                 null,
    status        enum ('ACTIVE', 'WITHDRAWN') not null,
    constraint UK2ty1xmrrgtn89xt7kyxx6ta7h
        unique (nickname),
    constraint UK6dotkott2kjsp8vw4d0m25fb7
        unique (email)
);

create table meetings
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)                  not null,
    updated_at  datetime(6)                  not null,
    deleted_at  datetime(6)                  null,
    description varchar(500)                 null,
    name        varchar(100)                 not null,
    status      enum ('ACTIVE', 'COMPLETED') not null,
    host_id     bigint                       not null,
    constraint FKsa773v03wufueg9de8cmjydr4
        foreign key (host_id) references users (id)
);

create table meeting_invitations
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)  not null,
    expires_at  datetime(6)  not null,
    invite_code varchar(255) not null,
    meeting_id  bigint       not null,
    constraint UK6gy9p6wperryh8ctnvnhn715v
        unique (invite_code),
    constraint FKss0w8fcnip72ucdup3iq5eq2u
        foreign key (meeting_id) references meetings (id)
);

create table meeting_members
(
    id         bigint auto_increment
        primary key,
    joined_at  datetime(6)             null,
    left_at    datetime(6)             null,
    status     enum ('JOINED', 'LEFT') not null,
    meeting_id bigint                  not null,
    user_id    bigint                  not null,
    constraint uk_meeting_member_meeting_user
        unique (meeting_id, user_id),
    constraint FK59dw9pwblepm19v2ybg20r0ap
        foreign key (user_id) references users (id),
    constraint FKf0yer61yyfen0a33nre5kkyv4
        foreign key (meeting_id) references meetings (id)
);

create table schedule_polls
(
    id         bigint auto_increment
        primary key,
    deadline   datetime(6)             not null,
    status     enum ('CLOSED', 'OPEN') not null,
    meeting_id bigint                  not null,
    constraint UK69w6wctniqg5a49sx9dfsi03w
        unique (meeting_id),
    constraint FKetsunusf9ysj4xxjq79ygl3kq
        foreign key (meeting_id) references meetings (id)
);

create table schedule_candidates
(
    id               bigint auto_increment
        primary key,
    created_at       datetime(6) not null,
    candidate_date   date        not null,
    schedule_poll_id bigint      not null,
    constraint uk_schedule_candidate_poll_date
        unique (schedule_poll_id, candidate_date),
    constraint FKe7p7e2b1av22ybq1b2g8dj5k6
        foreign key (schedule_poll_id) references schedule_polls (id)
);

create table schedule_votes
(
    id                    bigint auto_increment
        primary key,
    created_at            datetime(6)                                           not null,
    updated_at            datetime(6)                                           not null,
    preference            enum ('AVAILABLE', 'DISLIKE', 'IMPOSSIBLE', 'PREFER') not null,
    meeting_member_id     bigint                                                not null,
    schedule_candidate_id bigint                                                not null,
    constraint uk_schedule_vote_candidate_member
        unique (schedule_candidate_id, meeting_member_id),
    constraint FK8625bpeilf8udi6ljdnngmwcv
        foreign key (meeting_member_id) references meeting_members (id),
    constraint FKfdh5htfnvpmx3350825lwjx4i
        foreign key (schedule_candidate_id) references schedule_candidates (id)
);

