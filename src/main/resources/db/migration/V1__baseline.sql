--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)




--
-- Name: t_judge_servers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_judge_servers (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    updated_at timestamp(6) without time zone NOT NULL,
    updated_by character varying(255),
    cpu_core integer,
    cpu_usage double precision,
    hostname character varying(255) NOT NULL,
    ip character varying(255),
    is_disabled boolean NOT NULL,
    judger_version character varying(255),
    last_heartbeat timestamp(6) without time zone,
    memory_usage double precision,
    service_url character varying(255),
    task_number integer
);


--
-- Name: t_languages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_languages (
    is_disabled boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    compile_command character varying(255),
    created_by character varying(255),
    exe_name character varying(255),
    extension character varying(255),
    identifier character varying(255),
    name character varying(255),
    run_command character varying(255),
    src_name character varying(255),
    updated_by character varying(255),
    seccomp_rule character varying(255),
    max_memory bigint,
    compile_max_memory bigint,
    editor_format character varying(255)
);


--
-- Name: t_problem_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_problem_tags (
    problem_id uuid NOT NULL,
    tag character varying(255)
);


--
-- Name: t_problems; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_problems (
    hardness_level integer NOT NULL,
    memory_limit bigint NOT NULL,
    status integer NOT NULL,
    time_limit integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    created_by character varying(255),
    description text,
    hint character varying(255),
    input_description character varying(255),
    output_description character varying(255),
    problem_slug character varying(255),
    sample_input character varying(255),
    sample_output character varying(255),
    subject text,
    title character varying(255) NOT NULL,
    updated_by character varying(255)
);


--
-- Name: t_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_roles (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    updated_at timestamp(6) without time zone NOT NULL,
    updated_by character varying(255),
    description character varying(255),
    name character varying(255)
);


--
-- Name: t_submissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_submissions (
    cpu_time integer,
    status integer NOT NULL,
    "time" integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    memory bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    language_id uuid NOT NULL,
    problem_id uuid NOT NULL,
    created_by character varying(255),
    error_message character varying(255),
    result integer,
    source_code text,
    updated_by character varying(255),
    share_submission boolean,
    user_id uuid
);


--
-- Name: t_test_cases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_test_cases (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    problem_id uuid,
    created_by character varying(255),
    input text,
    output text,
    updated_by character varying(255)
);


--
-- Name: t_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_tokens (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    updated_at timestamp(6) without time zone NOT NULL,
    updated_by character varying(255),
    expired boolean NOT NULL,
    revoked boolean NOT NULL,
    token character varying(255),
    token_type character varying(255),
    user_id uuid,
    CONSTRAINT t_tokens_token_type_check CHECK (((token_type)::text = ANY (ARRAY[('ACCESS'::character varying)::text, ('REFRESH'::character varying)::text])))
);


--
-- Name: t_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    updated_at timestamp(6) without time zone NOT NULL,
    updated_by character varying(255),
    email character varying(255),
    enabled_mfa boolean,
    password character varying(255),
    status integer,
    username character varying(255),
    last_login timestamp(6) without time zone,
    avatar character varying(255),
    google_id character varying(255),
    name character varying(255)
);


--
-- Name: t_users_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.t_users_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL
);


--
-- Name: t_judge_servers t_judge_servers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_judge_servers
    ADD CONSTRAINT t_judge_servers_pkey PRIMARY KEY (id);


--
-- Name: t_languages t_languages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_languages
    ADD CONSTRAINT t_languages_pkey PRIMARY KEY (id);


--
-- Name: t_problems t_problems_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_problems
    ADD CONSTRAINT t_problems_pkey PRIMARY KEY (id);


--
-- Name: t_roles t_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_roles
    ADD CONSTRAINT t_roles_pkey PRIMARY KEY (id);


--
-- Name: t_submissions t_submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT t_submissions_pkey PRIMARY KEY (id);


--
-- Name: t_test_cases t_test_cases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_test_cases
    ADD CONSTRAINT t_test_cases_pkey PRIMARY KEY (id);


--
-- Name: t_tokens t_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_tokens
    ADD CONSTRAINT t_tokens_pkey PRIMARY KEY (id);


--
-- Name: t_users t_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_users
    ADD CONSTRAINT t_users_pkey PRIMARY KEY (id);


--
-- Name: t_users_roles t_users_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_users_roles
    ADD CONSTRAINT t_users_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: t_judge_servers uk212txd2uon50ymsndni3pssu9; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_judge_servers
    ADD CONSTRAINT uk212txd2uon50ymsndni3pssu9 UNIQUE (hostname);


--
-- Name: t_tokens ukdjdnp60wf0lq8erni3suse1np; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_tokens
    ADD CONSTRAINT ukdjdnp60wf0lq8erni3suse1np UNIQUE (token);


--
-- Name: t_submissions fk3i9f6nx3g74kxcljr76ta0gnc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT fk3i9f6nx3g74kxcljr76ta0gnc FOREIGN KEY (language_id) REFERENCES public.t_languages(id);


--
-- Name: t_users_roles fk4tbnlvd7naivo2om0ma842821; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_users_roles
    ADD CONSTRAINT fk4tbnlvd7naivo2om0ma842821 FOREIGN KEY (role_id) REFERENCES public.t_roles(id);


--
-- Name: t_tokens fk4yapf70j8ywq6xye5ypmr4a9g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_tokens
    ADD CONSTRAINT fk4yapf70j8ywq6xye5ypmr4a9g FOREIGN KEY (user_id) REFERENCES public.t_users(id);


--
-- Name: t_submissions fk6a65byiirmyaxvk5nropbc6rf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT fk6a65byiirmyaxvk5nropbc6rf FOREIGN KEY (problem_id) REFERENCES public.t_problems(id);


--
-- Name: t_problem_tags fka8tpg3m9rbbg4qqxfxj42rey0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_problem_tags
    ADD CONSTRAINT fka8tpg3m9rbbg4qqxfxj42rey0 FOREIGN KEY (problem_id) REFERENCES public.t_problems(id);


--
-- Name: t_submissions fkd08ypnjk6cmr3yrcvm9b8rdyk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT fkd08ypnjk6cmr3yrcvm9b8rdyk FOREIGN KEY (user_id) REFERENCES public.t_users(id);


--
-- Name: t_users_roles fkfxgldwdsgyl221kqaum2l0dm9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_users_roles
    ADD CONSTRAINT fkfxgldwdsgyl221kqaum2l0dm9 FOREIGN KEY (user_id) REFERENCES public.t_users(id);


--
-- Name: t_test_cases fkq4k9852xn1fv9b2b1p29na3jp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.t_test_cases
    ADD CONSTRAINT fkq4k9852xn1fv9b2b1p29na3jp FOREIGN KEY (problem_id) REFERENCES public.t_problems(id);


--
-- PostgreSQL database dump complete
--


