CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    completed_at DATETIME NULL,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE task_status_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    operator_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_task_status_logs_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_status_logs_operator FOREIGN KEY (operator_user_id) REFERENCES users(id)
);
