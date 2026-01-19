--
-- PostgreSQL database dump
--

\restrict Py8a0KqL1chN00bx7aavKMtEfLh6XociEcXU9VeRhVvCk9p00WbIlrFZYWO2be9

-- Dumped from database version 16.11 (Ubuntu 16.11-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.11 (Ubuntu 16.11-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: t_languages; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.t_languages OWNER TO postgres;

--
-- Name: t_problems; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.t_problems OWNER TO postgres;

--
-- Name: t_roles; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.t_roles OWNER TO postgres;

--
-- Name: t_submissions; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.t_submissions OWNER TO postgres;

--
-- Name: t_test_cases; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.t_test_cases OWNER TO postgres;

--
-- Name: t_tokens; Type: TABLE; Schema: public; Owner: postgres
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
    CONSTRAINT t_tokens_token_type_check CHECK (((token_type)::text = ANY ((ARRAY['ACCESS'::character varying, 'REFRESH'::character varying])::text[])))
);


ALTER TABLE public.t_tokens OWNER TO postgres;

--
-- Name: t_users; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.t_users OWNER TO postgres;

--
-- Name: t_users_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.t_users_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL
);


ALTER TABLE public.t_users_roles OWNER TO postgres;

--
-- Data for Name: t_languages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_languages (is_disabled, created_at, updated_at, id, compile_command, created_by, exe_name, extension, identifier, name, run_command, src_name, updated_by, seccomp_rule, max_memory, compile_max_memory, editor_format) FROM stdin;
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	4eb57dbb-d7d8-4589-ad49-0e2d5691ba52	\N	system	solution.php	php	php	PHP	/usr/bin/php {exe_path}	solution.php	system		134217728	\N	php
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	7e9d29aa-2c49-487c-93d0-fe019c608e4e	\N	system	solution.js	js	javascript	JavaScript	/usr/bin/node {exe_path}	solution.js	system		134217728	\N	javascript
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	8eb51c84-2d03-4f86-92c3-1f31a671ff12	/usr/bin/gcc -DONLINE_JUDGE -O2 -w -fmax-errors=3 -std=c99 {src_path} -lm -o {exe_path}	system	main	c	c	C	{exe_path}	main.c	system	c_cpp	134217728	134217728	c
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	5c3a9dc8-9b97-48af-ae20-b3910b0e3ce9	/usr/bin/python -m py_compile {src_path}	system	solution.pyc	py	python2	Python 2	/usr/bin/python {exe_path}	solution.py	system	general	134217728	134217728	python
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	84cebb99-dd2c-4fd9-a39b-69952b555a59	/usr/bin/python3 -m py_compile {src_path}	system	__pycache__/solution.cpython-36.pyc	py	python3	Python 3	/usr/bin/python3 {exe_path}	solution.py	system	general	134217728	134217728	python
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	c6a8cb0d-9d88-449f-a7af-61916b27599f	/usr/bin/go build -o {exe_path} {src_path}	system	main	go	go	Go	{exe_path}	main.go	system		134217728	134217728	go
f	2026-01-06 22:23:59.844851	2026-01-06 22:23:59.844851	34dc479c-7d2e-4b0e-9737-32e72107854c	/usr/bin/javac {src_path} -d {exe_dir} -encoding UTF8	system	Main	java	java	Java	/usr/bin/java -cp {exe_dir} -Xss1M -Xmx256m -Djava.awt.headless=true Main	Main.java	system	\N	268435456	-1	java
f	2026-01-04 10:23:45	2026-01-04 10:23:45	81c300cf-4c19-407c-a3c6-de19866d75ed	/usr/bin/g++ -DONLINE_JUDGE -O2 -w -fmax-errors=3 -std=c++17 {src_path} -lm -o {exe_path}	\N	main	\N	cpp	C++	{exe_path}	main.cpp	\N	\N	134217728	134217728	cpp
\.


--
-- Data for Name: t_problems; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_problems (hardness_level, memory_limit, status, time_limit, created_at, updated_at, id, created_by, description, hint, input_description, output_description, problem_slug, sample_input, sample_output, subject, title, updated_by) FROM stdin;
1	256	0	1000	2026-01-05 22:26:42.56581	2026-01-05 22:26:42.56581	6347d5d8-e782-468e-814f-52af5a2c8f57	\N	Ở vương quốc "Số Học", có một ngọn núi cao tên là Olympus. Để leo lên đỉnh núi, các nhà thám hiểm phải bước qua những bậc thang đá cổ xưa. Tuy nhiên, ngọn núi này có một lời nguyền: Chỉ những con số mang hình dáng của một "chiếc cầu thang đi lên" mới có thể mở cửa kho báu trên đỉnh núi.\nMột con số được gọi là Số bậc thang nếu các chữ số của nó (tính từ trái sang phải) luôn giữ vững phong độ: chữ số đứng sau phải lớn hơn chữ số đứng trước. Ví dụ, số 125 là số bậc thang, nhưng số 132 thì không vì từ 3 lại tụt xuống 2.\nHãy giúp các nhà thám hiểm kiểm tra xem mật mã họ tìm được có phải là chiếc chìa khóa để mở cửa kho báu không nhé!	Duyệt từng cặp chữ số liên tiếp từ trái sang phải và kiểm tra chữ số sau có lớn hơn chữ số trước hay không.	Một dòng duy nhất chứa số nguyên dương N (10 ≤ N ≤ 10^18).	In ra YES nếu N là số bậc thang.\nIn ra NO nếu N không phải là số bậc thang.	bac-thang-danh-vong	12379	YES	Cho một số nguyên dương N.\nHãy kiểm tra xem N có phải là số bậc thang (các chữ số tăng dần nghiêm ngặt từ trái sang phải) hay không.\n	BẬC THANG DANH VỌNG	\N
1	256	0	1000	2026-01-05 22:39:12.745512	2026-01-05 22:39:12.745512	bb355243-e789-45ee-ad67-016a8ba1ee18	\N	Calculate A + B	\N	Two integers separated by spaces.	Sum of two numbers	simple-a-plus-b	1 1	2	Please calculate the sum of two integers and output the result.\n\nBe careful not to have unnecessary output, such as "Please enter the values of a and b:". See the hidden part for sample code.\n\n\nInput\n\nTwo integers separated by spaces.\n\n\nOutput\n\nSum of two numbers	Simple A + B Problem	\N
1	128	0	1000	2026-01-04 12:27:09.453777	2026-01-04 12:27:09.453777	23408310-414d-4438-9844-dc8f15f04d47	\N			Một dòng duy nhất chứa hai số nguyên dương a và b, cách nhau một khoảng trắng.	Một số nguyên duy nhất là ước số chung lớn nhất của a và b.	ucln-a-b	12 18	6	Cho hai số nguyên dương a và b.\nHãy tìm Ước số chung lớn nhất (ƯCLN) của hai số đó.\n\nƯớc số chung lớn nhất của hai số nguyên là số nguyên dương lớn nhất mà cả hai số đó đều chia hết.	TÌM ƯỚC SỐ CHUNG LỚN NHẤT	\N
\.


--
-- Data for Name: t_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_roles (id, created_at, created_by, updated_at, updated_by, description, name) FROM stdin;
b461277c-a4cb-44a4-92db-9185a1bade5e	2026-01-16 10:20:06	SYS	2026-01-16 10:20:18	SYS	User role	USER
626c2558-a7d5-4fa4-9161-9bec2ae74076	2026-01-16 10:20:53	SYS	2026-01-16 10:20:59	SYS	Admin role	ADMIN
\.


--
-- Data for Name: t_submissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_submissions (cpu_time, status, "time", created_at, memory, updated_at, id, language_id, problem_id, created_by, error_message, result, source_code, updated_by, share_submission, user_id) FROM stdin;
\N	-1	0	2026-01-19 10:07:16.258955	0	2026-01-19 10:07:16.29141	f3603481-eb81-4ff3-946d-0b26fefcfdd5	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	I/O error on POST request for "http://localhost:8080/judge": null	\N	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b;\n        swap(a, b);\n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << '\\n';\n    }\n\n    return 0;\n}\n	\N	f	00b564b7-5869-476e-ac39-137d827d141b
0	0	4	2026-01-19 10:07:55.039648	3575808	2026-01-19 10:07:55.770682	74181722-b9f2-4def-93af-914068092804	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b;\n        swap(a, b);\n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << '\\n';\n    }\n\n    return 0;\n}\n	\N	f	00b564b7-5869-476e-ac39-137d827d141b
0	0	9	2026-01-19 10:09:53.446893	3579904	2026-01-19 10:09:53.813278	1273db88-6ac2-4233-9c3c-11da9ea14c66	81c300cf-4c19-407c-a3c6-de19866d75ed	6347d5d8-e782-468e-814f-52af5a2c8f57	\N	\N	0	#include <iostream>\n#include <string>\n\nusing namespace std;\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    string n;\n    cin >> n;\n\n    bool isStepNumber = true;\n\n    for (int i = 0; i < n.length() - 1; i++) {\n        if (n[i] >= n[i+1]) {\n            isStepNumber = false;\n            break;\n        }\n    }\n\n    if (isStepNumber) {\n        cout << "YES" << endl;\n    } else {\n        cout << "NO" << endl;\n    }\n\n    return 0;\n}	\N	f	00b564b7-5869-476e-ac39-137d827d141b
\N	-1	0	2026-01-04 12:50:52.390948	3350528	2026-01-04 12:52:24.340472	0f9c6d1a-dcff-4f4f-9be1-d3aca9446226	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	URI with undefined scheme	2	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
\N	-1	0	2026-01-04 12:58:44.114129	3350528	2026-01-04 12:58:44.201702	40189d3a-2f48-480c-924d-d9321d43fbfd	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	I/O error on POST request for "http://localhost:8080/judge": null	2	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
4	0	5	2026-01-04 14:03:15.582752	3538944	2026-01-04 14:03:16.222877	668a733c-a097-4b9c-a262-d499de6f143f	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
0	0	3	2026-01-04 14:21:37.806674	3563520	2026-01-04 14:21:38.603442	8c3adb0a-eb06-40f3-a8c9-c8461ba86677	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
9	-1	169	2026-01-06 22:02:01.247156	135540736	2026-01-06 22:02:01.897807	cc226dd1-6504-449c-b32b-1c795fb88a24	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	#include <iostream>\n#include <vector>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    // Cấp phát bộ nhớ liên tục trong vòng lặp vô hạn\n    // Mỗi lần 1MB, và ghi vào đó để ép OS cấp page thực sự\n    vector<int*> leak;\n    while (true) {\n        int* p = new int[250000]; // ~1MB\n        for (int i = 0; i < 250000; i += 4096) {\n            p[i] = 1; // Touch memory to force allocation\n        }\n        leak.push_back(p);\n    }\n\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
1	0	4	2026-01-06 22:03:49.782341	3624960	2026-01-06 22:03:50.260988	bb81ecfb-49b0-4453-9c3d-ac5b08c29966	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\n// Khai báo mảng toàn cục lớn (~152MB)\n// int = 4 bytes. 40,000,000 * 4 = 160,000,000 bytes ≈ 152 MB\n// Vượt quá giới hạn 128MB thông thường\nint huge_memory[40000000];\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b);\n    }\n    return a;\n}\n\nint main() {\n    // Truy cập mảng để đảm bảo OS thực sự map memory (tránh lazy allocation)\n    huge_memory[0] = 1;\n    huge_memory[39999999] = 2;\n\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
0	-1	4	2026-01-06 22:00:38.316835	3350528	2026-01-06 22:00:39.133119	0bf51eea-419e-471f-9efc-90b7500066fe	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	#include <iostream>\n#include <algorithm>\n#include <vector>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    // Cố tình cấp phát mảng lớn (~800MB - 1.6GB) để gây lỗi MEMORY_LIMIT_EXCEEDED\n    vector<long long> memoryLeak(200000000, 1);\n\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
6	-1	25	2026-01-06 22:40:51.921169	17567744	2026-01-06 22:40:52.579385	c5b35ab8-f354-4f6d-a00d-7b16b5754dd2	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
\N	-1	0	2026-01-04 13:00:31.625679	3350528	2026-01-04 13:00:31.651296	2950de47-ee51-4f05-b537-542620e85949	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	Error while extracting response for type [vn.thanhtuanle.judge.dto.JudgeResponseDto] and content type [application/json]	3	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
2	-1	6	2026-01-06 22:33:07.635544	17268736	2026-01-06 22:33:08.312914	61bd3bf0-4afe-423a-9fc0-33a262e5b39d	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
3	-1	9	2026-01-06 22:36:17.476145	17657856	2026-01-06 22:36:18.209905	d294994d-24f1-4d8b-80bf-e41b915f8df3	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
1	0	6	2026-01-11 17:51:14.040137	1462272	2026-01-11 17:51:14.38757	b15f2206-27ed-406d-90ae-22e42a53d4a9	8eb51c84-2d03-4f86-92c3-1f31a671ff12	bb355243-e789-45ee-ad67-016a8ba1ee18	\N	\N	0	#include <stdio.h>\n\nint main() {\n    int a, b;\n\n    if (scanf("%d %d", &a, &b) == 2) {\n        printf("%d\\n", a + b);\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
0	0	3	2026-01-04 14:32:23.978218	3530752	2026-01-04 14:32:24.682984	37e23f99-dfaf-40df-b900-52b40ee68211	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
1	0	4	2026-01-04 21:24:11.013665	3477504	2026-01-04 21:24:11.698199	26bef3d2-0994-4b02-b6f2-05fdcad53f50	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
0	0	2	2026-01-04 21:32:10.459186	3522560	2026-01-04 21:32:11.081387	936e09e0-1c30-47b2-89ae-4bfc61a4da9b	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
2	-1	8	2026-01-06 22:37:52.771343	18419712	2026-01-06 22:37:53.322249	d774cb40-cee6-4da7-bf2b-e7e2705d3cda	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
6	-1	9	2026-01-06 22:38:18.053376	13488128	2026-01-06 22:38:18.610354	1323165c-5dff-4817-a53c-eaeb797dd578	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
2	-1	6	2026-01-06 22:47:44.434548	17510400	2026-01-06 22:47:45.144418	11fb79a8-4d73-4bcc-a55b-69137ea2ce33	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
3	-1	7	2026-01-06 22:52:25.703154	17453056	2026-01-06 22:52:26.276916	fbc788d6-1988-41ab-abe7-094f09e491c9	34dc479c-7d2e-4b0e-9737-32e72107854c	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextLong()) {\n            long a = scanner.nextLong();\n            long b = scanner.nextLong();\n            System.out.println(gcd(a, b));\n        }\n    }\n\n    private static long gcd(long a, long b) {\n        while (b != 0) {\n            long temp = b;\n            b = a % b;\n            a = temp;\n        }\n        return a;\n    }\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
1	0	3	2026-01-06 22:53:35.856816	3584000	2026-01-06 22:53:36.373889	f834d7ac-e392-46b2-855f-97d6cbde6b27	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	\N	0	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
\N	-1	0	2026-01-04 13:05:13.694465	3350528	2026-01-04 13:05:13.771821	78226cbe-c614-4cf8-8f8b-fc6d8de66fbb	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	JudgeClientError	4	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
\N	-1	0	2026-01-04 13:37:06.992419	3350528	2026-01-04 13:37:07.07627	19cbb72d-b86a-4a2c-a1cf-28aab1266755	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	JudgeClientError	5	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
\N	-1	0	2026-01-04 13:57:48.890538	3350528	2026-01-04 13:57:49.007456	b8407bb1-9672-4153-a8f5-c12c8ef04e83	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	JudgeClientError	5	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
\N	-1	0	2026-01-04 14:01:55.452014	3350528	2026-01-04 14:01:55.586446	365b047f-10f6-45c3-bdb5-4c93200f7f64	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	JudgeClientError: TypeError :judge() got an unexpected keyword argument 'user_id'	5	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
\N	-1	0	2026-01-04 21:23:21.740952	3350528	2026-01-04 21:23:21.817906	0c89a29a-935d-480a-89f1-ef48ee0680b1	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	I/O error on POST request for "http://localhost:8080/judge": null	2	#include <iostream>\n#include <algorithm>\n\nusing namespace std;\n\nlong long findGCD(long long a, long long b) {\n    while (b != 0) {\n        a = a % b; \n        swap(a, b); \n    }\n    return a;\n}\n\nint main() {\n    ios_base::sync_with_stdio(false);\n    cin.tie(NULL);\n\n    long long a, b;\n    if (cin >> a >> b) {\n        cout << findGCD(a, b) << endl;\n    }\n\n    return 0;\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
\N	-1	0	2026-01-11 16:34:33.88887	0	2026-01-11 16:34:33.945074	5656ceda-589b-44ee-81fc-5dd9f32d1cbd	81c300cf-4c19-407c-a3c6-de19866d75ed	23408310-414d-4438-9844-dc8f15f04d47	\N	I/O error on POST request for "http://localhost:8080/judge": null	\N	#include <iostream>\\n#include <algorithm>\\n\\nusing namespace std;\\n\\nlong long findGCD(long long a, long long b) {\\n    while (b != 0) {\\n        a = a % b; \\n        swap(a, b); \\n    }\\n    return a;\\n}\\n\\nint main() {\\n    ios_base::sync_with_stdio(false);\\n    cin.tie(NULL);\\n\\n    long long a, b;\\n    if (cin >> a >> b) {\\n        cout << findGCD(a, b) << endl;\\n    }\\n\\n    return 0;\\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
0	0	1	2026-01-11 17:58:16.237124	1482752	2026-01-11 17:58:16.415741	8eee7597-aa9c-4a94-9662-dcd190ee11b1	8eb51c84-2d03-4f86-92c3-1f31a671ff12	bb355243-e789-45ee-ad67-016a8ba1ee18	\N	\N	0	#include <stdio.h>\n\nint main() {\n    int a, b;\n\n    // Đọc dữ liệu từ bàn phím\n    if (scanf("%d %d", &a, &b) == 2) {\n        // In kết quả tổng của a và b\n        printf("%d\\n", a + b);\n    }\n\n    return 0;\n}	\N	f	8771511f-fd22-405a-91f9-184bb40cd947
0	-1	6	2026-01-11 18:06:42.459361	17395712	2026-01-11 18:06:43.178447	96ce556f-fd81-4148-b79a-7d1805b4ba59	34dc479c-7d2e-4b0e-9737-32e72107854c	bb355243-e789-45ee-ad67-016a8ba1ee18	\N	\N	4	import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n\n        try {\n            int a = scanner.nextInt();\n            int b = scanner.nextInt();\n\n            int sum = a + b;\n            System.out.println(sum);\n        } catch (Exception e) {\n            System.err.println("Vui lòng nhập số nguyên hợp lệ!");\n        } finally {\n            scanner.close();\n        }\n    }\n}	\N	f	4953467e-4319-4367-a26b-9d7e1de54db8
\.


--
-- Data for Name: t_test_cases; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_test_cases (created_at, updated_at, id, problem_id, created_by, input, output, updated_by) FROM stdin;
2026-01-04 12:27:09.479017	2026-01-04 12:27:09.479017	564117cf-c49a-4175-8bfd-5c3480915729	23408310-414d-4438-9844-dc8f15f04d47	\N	ucln-a-b/1.in	ucln-a-b/1.out	\N
2026-01-04 12:27:09.480719	2026-01-04 12:27:09.480719	42cff460-03f9-4419-8540-47d97dd0603f	23408310-414d-4438-9844-dc8f15f04d47	\N	ucln-a-b/2.in	ucln-a-b/2.out	\N
2026-01-04 12:27:09.480844	2026-01-04 12:27:09.480844	53b026ea-828d-4e6c-822b-70b3e346d1c8	23408310-414d-4438-9844-dc8f15f04d47	\N	ucln-a-b/3.in	ucln-a-b/3.out	\N
2026-01-05 22:26:42.590735	2026-01-05 22:26:42.590735	1d2ab01f-242f-40b2-9ce4-f36ccdde2016	6347d5d8-e782-468e-814f-52af5a2c8f57	\N	bac-thang-danh-vong/1.in	bac-thang-danh-vong/1.out	\N
2026-01-05 22:26:42.593036	2026-01-05 22:26:42.593036	2ca5a008-34a9-4d00-9dc6-2333436fede9	6347d5d8-e782-468e-814f-52af5a2c8f57	\N	bac-thang-danh-vong/2.in	bac-thang-danh-vong/2.out	\N
2026-01-05 22:26:42.593256	2026-01-05 22:26:42.593256	d8af9379-f849-48d4-8634-b106220b0883	6347d5d8-e782-468e-814f-52af5a2c8f57	\N	bac-thang-danh-vong/3.in	bac-thang-danh-vong/3.out	\N
2026-01-05 22:39:12.762633	2026-01-05 22:39:12.762633	4070f027-214f-4211-84f3-0b4f0779857d	bb355243-e789-45ee-ad67-016a8ba1ee18	\N	simple-a-plus-b/1.in	simple-a-plus-b/1.out	\N
\.


--
-- Data for Name: t_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_tokens (id, created_at, created_by, updated_at, updated_by, expired, revoked, token, token_type, user_id) FROM stdin;
59fc2a3c-b9ca-44b8-b686-e2042ccd6245	2026-01-19 10:05:14.84341	\N	2026-01-19 10:05:14.84341	\N	f	f	eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJVU0VSIl0sInN1YiI6InR1YW5sdDkzOUBnbWFpbC5jb20iLCJpYXQiOjE3Njg3OTE5MTQsImV4cCI6MTc2ODg3ODMxNH0.VRLLTT-Ewk5tJ2I2F4rye98pvgwU_X_NusmBFNmRKPw	ACCESS	00b564b7-5869-476e-ac39-137d827d141b
688e66f1-0803-4903-9fa5-8a0516b66c32	2026-01-19 10:05:14.844888	\N	2026-01-19 10:05:14.844888	\N	f	f	eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0dWFubHQ5MzlAZ21haWwuY29tIiwiaWF0IjoxNzY4NzkxOTE0LCJleHAiOjE3NjkwNTExMTR9.tjCSpOKCMxPBTQWDx-lUKdsM7ln69-9bx3lhEyGnMOE	REFRESH	00b564b7-5869-476e-ac39-137d827d141b
\.


--
-- Data for Name: t_users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_users (id, created_at, created_by, updated_at, updated_by, email, enabled_mfa, password, status, username, last_login, avatar, google_id, name) FROM stdin;
4953467e-4319-4367-a26b-9d7e1de54db8	2026-01-18 11:07:27.489218	\N	2026-01-18 11:39:01.472269	\N	thanhtuanle0209@gmail.com	\N	\N	1	thanhtuanle0209@gmail.com	2026-01-18 11:39:01.466953	https://lh3.googleusercontent.com/a/ACg8ocK3G4u6_dGvlyclMGiHxMZG2pACSh_osL9X8vtxIu1Vr9ebwtWB=s96-c	106925391457372337585	tuan lethanh
8771511f-fd22-405a-91f9-184bb40cd947	2026-01-18 10:42:12.28591	\N	2026-01-18 22:47:29.436202	\N	thanhtuanle939@gmail.com	\N	\N	1	thanhtuanle939@gmail.com	2026-01-18 22:47:29.432416	https://lh3.googleusercontent.com/a/ACg8ocJeFSKKBMzsThe0us-LVao3tQlTT6U24QRqbh661Cq9VOzCEMZz=s96-c	112248274275698442374	Lê Thanh Tuấn
00b564b7-5869-476e-ac39-137d827d141b	2026-01-19 10:05:14.819279	\N	2026-01-19 10:05:14.8471	\N	tuanlt939@gmail.com	\N	\N	1	tuanlt939@gmail.com	2026-01-19 10:05:14.845014	https://lh3.googleusercontent.com/a/ACg8ocJKNg6eUGpIrYhFUlDTpNt5GbwfPTIy8eqd4gw3HFTgnjSvDyw=s96-c	112736527545321527906	LT Tuan
\.


--
-- Data for Name: t_users_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.t_users_roles (user_id, role_id) FROM stdin;
8771511f-fd22-405a-91f9-184bb40cd947	b461277c-a4cb-44a4-92db-9185a1bade5e
4953467e-4319-4367-a26b-9d7e1de54db8	b461277c-a4cb-44a4-92db-9185a1bade5e
00b564b7-5869-476e-ac39-137d827d141b	b461277c-a4cb-44a4-92db-9185a1bade5e
\.


--
-- Name: t_languages t_languages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_languages
    ADD CONSTRAINT t_languages_pkey PRIMARY KEY (id);


--
-- Name: t_problems t_problems_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_problems
    ADD CONSTRAINT t_problems_pkey PRIMARY KEY (id);


--
-- Name: t_roles t_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_roles
    ADD CONSTRAINT t_roles_pkey PRIMARY KEY (id);


--
-- Name: t_submissions t_submissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT t_submissions_pkey PRIMARY KEY (id);


--
-- Name: t_test_cases t_test_cases_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_test_cases
    ADD CONSTRAINT t_test_cases_pkey PRIMARY KEY (id);


--
-- Name: t_tokens t_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_tokens
    ADD CONSTRAINT t_tokens_pkey PRIMARY KEY (id);


--
-- Name: t_users t_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_users
    ADD CONSTRAINT t_users_pkey PRIMARY KEY (id);


--
-- Name: t_users_roles t_users_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_users_roles
    ADD CONSTRAINT t_users_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: t_tokens ukdjdnp60wf0lq8erni3suse1np; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_tokens
    ADD CONSTRAINT ukdjdnp60wf0lq8erni3suse1np UNIQUE (token);


--
-- Name: t_submissions fk3i9f6nx3g74kxcljr76ta0gnc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT fk3i9f6nx3g74kxcljr76ta0gnc FOREIGN KEY (language_id) REFERENCES public.t_languages(id);


--
-- Name: t_users_roles fk4tbnlvd7naivo2om0ma842821; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_users_roles
    ADD CONSTRAINT fk4tbnlvd7naivo2om0ma842821 FOREIGN KEY (role_id) REFERENCES public.t_roles(id);


--
-- Name: t_tokens fk4yapf70j8ywq6xye5ypmr4a9g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_tokens
    ADD CONSTRAINT fk4yapf70j8ywq6xye5ypmr4a9g FOREIGN KEY (user_id) REFERENCES public.t_users(id);


--
-- Name: t_submissions fk6a65byiirmyaxvk5nropbc6rf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT fk6a65byiirmyaxvk5nropbc6rf FOREIGN KEY (problem_id) REFERENCES public.t_problems(id);


--
-- Name: t_submissions fkd08ypnjk6cmr3yrcvm9b8rdyk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_submissions
    ADD CONSTRAINT fkd08ypnjk6cmr3yrcvm9b8rdyk FOREIGN KEY (user_id) REFERENCES public.t_users(id);


--
-- Name: t_users_roles fkfxgldwdsgyl221kqaum2l0dm9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_users_roles
    ADD CONSTRAINT fkfxgldwdsgyl221kqaum2l0dm9 FOREIGN KEY (user_id) REFERENCES public.t_users(id);


--
-- Name: t_test_cases fkq4k9852xn1fv9b2b1p29na3jp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.t_test_cases
    ADD CONSTRAINT fkq4k9852xn1fv9b2b1p29na3jp FOREIGN KEY (problem_id) REFERENCES public.t_problems(id);


--
-- PostgreSQL database dump complete
--

\unrestrict Py8a0KqL1chN00bx7aavKMtEfLh6XociEcXU9VeRhVvCk9p00WbIlrFZYWO2be9

